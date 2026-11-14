package net.mypapit.mobile.myrepeater.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.Network
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.expressions.Expression.toString as asString
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val ONLINE_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val OFFLINE_STYLE = "asset://map_style_offline.json"

private const val SOURCE_SITES = "repeater-sites"
private const val SOURCE_USER = "user-position"
private const val LAYER_CLUSTERS = "repeater-clusters"
private const val LAYER_CLUSTER_COUNT = "repeater-cluster-count"
private const val LAYER_SITES = "repeater-pins"
private const val LAYER_USER = "user-position-dot"

private const val PROP_SITE_ID = "site"
private const val PROP_BAND = "band"
private const val PROP_ON_AIR = "onAir"
private const val PROP_COUNT = "count"

private const val COLOR_2M = "#FF8500"
private const val COLOR_70CM = "#33B5E5"
private const val COLOR_MIXED = "#C9A0FF"
private const val COLOR_USER = "#6DD58C"
private const val COLOR_OUTLINE = "#121110"

private const val TAP_SLOP_PX = 24f
private const val STYLE_TIMEOUT_MS = 4_000L
private const val USER_ZOOM = 8.5
private const val FOCUS_ZOOM = 12.0
private const val CLUSTER_MAX_ZOOM = 11

/**
 * The map itself: repeater pins (clustered when zoomed out), the user's
 * position, and tap handling. Tiles come from OpenFreeMap; if its style does not
 * load in time (first use with no connection), a plain dark background is used
 * so the pins still show, and the online style is retried when a connection returns.
 *
 * @param focus where to centre the camera when the map first opens, if known
 * @param savedCamera latitude, longitude and zoom to restore when coming back to the map
 * @param flyTo a place to move the camera to now; [onFlown] is called once that has started
 */
@Composable
fun RepeaterMapView(
    sites: List<MapSite>,
    userPosition: GeoPoint?,
    focus: GeoPoint?,
    savedCamera: DoubleArray?,
    flyTo: GeoPoint?,
    onFlown: () -> Unit,
    onCameraIdle: (DoubleArray) -> Unit,
    onSiteTapped: (MapSite?) -> Unit,
    onBasemapUnavailable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    val currentSites by rememberUpdatedState(sites)
    val currentOnTap by rememberUpdatedState(onSiteTapped)
    val currentOnUnavailable by rememberUpdatedState(onBasemapUnavailable)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    val currentOnFlown by rememberUpdatedState(onFlown)
    val margin = with(LocalDensity.current) { 12.dp.roundToPx() }

    MapLifecycle(mapView)

    DisposableEffect(mapView) {
        mapView.getMapAsync { ready ->
            map = ready
            ready.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isLogoEnabled = false
                attributionGravity = Gravity.BOTTOM or Gravity.START
                setAttributionMargins(margin, margin, margin, margin)
            }
            ready.moveCamera(
                savedCamera?.let { CameraUpdateFactory.newLatLngZoom(LatLng(it[0], it[1]), it[2]) }
                    ?: initialCamera(focus),
            )
            ready.addOnCameraIdleListener {
                val camera = ready.cameraPosition
                camera.target?.let { currentOnCameraIdle(doubleArrayOf(it.latitude, it.longitude, camera.zoom)) }
            }
            ready.addOnMapClickListener { tapped ->
                handleTap(ready, tapped, currentSites, currentOnTap)
                true
            }
        }
        onDispose { }
    }

    // Try the online style (served from the tile cache when offline). If it has not
    // arrived in time, show the bundled plain style so the pins are never missing.
    var attempt by remember { mutableIntStateOf(0) }
    var onFallback by remember { mutableStateOf(false) }
    LaunchedEffect(map, attempt) {
        val ready = map ?: return@LaunchedEffect
        var loaded = false
        style = null
        ready.setStyle(Style.Builder().fromUri(ONLINE_STYLE)) {
            loaded = true
            onFallback = false
            style = it
            currentOnUnavailable(false)
        }
        delay(STYLE_TIMEOUT_MS)
        if (!loaded) {
            onFallback = true
            currentOnUnavailable(true)
            ready.setStyle(Style.Builder().fromUri(OFFLINE_STYLE)) { style = it }
        }
    }

    // When a connection comes back while the plain style is showing, try again.
    DisposableEffect(mapView) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mapView.post { if (onFallback) attempt++ }
            }
        }
        connectivity?.registerDefaultNetworkCallback(callback)
        onDispose { connectivity?.unregisterNetworkCallback(callback) }
    }

    LaunchedEffect(map, flyTo) {
        val ready = map ?: return@LaunchedEffect
        val target = flyTo ?: return@LaunchedEffect
        ready.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), FOCUS_ZOOM))
        currentOnFlown()
    }

    // (Re)build our layers whenever a style finishes loading, then keep the data current.
    LaunchedEffect(style) {
        style?.let { addRepeaterLayers(it, withClusterCounts = !onFallback) }
    }
    LaunchedEffect(style, sites) {
        style?.getSourceAs<GeoJsonSource>(SOURCE_SITES)?.setGeoJson(sites.toFeatureCollection())
    }
    LaunchedEffect(style, userPosition) {
        style?.getSourceAs<GeoJsonSource>(SOURCE_USER)?.setGeoJson(
            FeatureCollection.fromFeatures(
                listOfNotNull(userPosition?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }),
            ),
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createMapView(context: Context): MapView {
    MapLibre.getInstance(context)
    // Dark while the style loads, instead of the library default light grey.
    val options = MapLibreMapOptions.createFromAttributes(context).foregroundLoadColor(Color.parseColor(COLOR_OUTLINE))
    return MapView(context, options)
}

private fun initialCamera(focus: GeoPoint?) =
    if (focus != null) {
        CameraUpdateFactory.newLatLngZoom(LatLng(focus.latitude, focus.longitude), USER_ZOOM)
    } else {
        val sw = MapSites.MALAYSIA_SOUTH_WEST
        val ne = MapSites.MALAYSIA_NORTH_EAST
        CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.Builder()
                .include(LatLng(sw.latitude, sw.longitude))
                .include(LatLng(ne.latitude, ne.longitude))
                .build(),
            48,
        )
    }

/**
 * @param withClusterCounts draw the number on each cluster. The digits need glyphs from the tile
 *   server, and a source whose glyphs cannot be fetched draws nothing at all, so the plain offline
 *   style must leave them out.
 */
private fun addRepeaterLayers(style: Style, withClusterCounts: Boolean) {
    if (style.getSource(SOURCE_SITES) != null) return

    style.addSource(
        GeoJsonSource(
            SOURCE_SITES,
            FeatureCollection.fromFeatures(emptyList()),
            GeoJsonOptions().withCluster(true).withClusterMaxZoom(CLUSTER_MAX_ZOOM).withClusterRadius(40),
        ),
    )
    style.addSource(GeoJsonSource(SOURCE_USER, FeatureCollection.fromFeatures(emptyList())))

    style.addLayer(
        CircleLayer(LAYER_CLUSTERS, SOURCE_SITES).withProperties(
            circleColor(Color.parseColor(COLOR_2M)),
            circleOpacity(0.85f),
            circleRadius(step(get("point_count"), literal(16f), stop(5, 20f), stop(15, 26f))),
            circleStrokeColor(Color.parseColor(COLOR_OUTLINE)),
            circleStrokeWidth(2f),
        ).withFilter(has("point_count")),
    )
    if (withClusterCounts) {
        style.addLayer(
        SymbolLayer(LAYER_CLUSTER_COUNT, SOURCE_SITES).withProperties(
            textField(asString(get("point_count"))),
            textFont(arrayOf("Noto Sans Bold")),
            textSize(13f),
            textColor(Color.parseColor(COLOR_OUTLINE)),
            textAllowOverlap(true),
        ).withFilter(has("point_count")),
        )
    }
    style.addLayer(
        CircleLayer(LAYER_SITES, SOURCE_SITES).withProperties(
            circleColor(
                switchCase(
                    eq(get(PROP_BAND), literal("2m")), literal(COLOR_2M),
                    eq(get(PROP_BAND), literal("70cm")), literal(COLOR_70CM),
                    literal(COLOR_MIXED),
                ),
            ),
            circleOpacity(switchCase(eq(get(PROP_ON_AIR), literal(true)), literal(1f), literal(0.45f))),
            circleRadius(switchCase(eq(get(PROP_COUNT), literal(1)), literal(8f), literal(10f))),
            circleStrokeColor(Color.parseColor(COLOR_OUTLINE)),
            circleStrokeWidth(2f),
        ).withFilter(not(has("point_count"))),
    )
    style.addLayer(
        CircleLayer(LAYER_USER, SOURCE_USER).withProperties(
            circleColor(Color.parseColor(COLOR_USER)),
            circleRadius(7f),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(2.5f),
        ),
    )
}

private fun List<MapSite>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(
        map { site ->
            Feature.fromGeometry(Point.fromLngLat(site.point.longitude, site.point.latitude)).apply {
                addStringProperty(PROP_SITE_ID, site.id)
                addStringProperty(PROP_BAND, site.band)
                addBooleanProperty(PROP_ON_AIR, site.onAir)
                addNumberProperty(PROP_COUNT, site.repeaters.size)
            }
        },
    )

/** A tap on a pin selects its site, a tap on a cluster zooms into it, and a tap elsewhere clears the selection. */
private fun handleTap(map: MapLibreMap, tapped: LatLng, sites: List<MapSite>, onSite: (MapSite?) -> Unit) {
    val screen: PointF = map.projection.toScreenLocation(tapped)
    val area = RectF(screen.x - TAP_SLOP_PX, screen.y - TAP_SLOP_PX, screen.x + TAP_SLOP_PX, screen.y + TAP_SLOP_PX)

    val pin = map.queryRenderedFeatures(area, LAYER_SITES).firstOrNull()
    if (pin != null) {
        val id = pin.getStringProperty(PROP_SITE_ID)
        onSite(sites.firstOrNull { it.id == id })
        return
    }

    val cluster = map.queryRenderedFeatures(area, LAYER_CLUSTERS).firstOrNull()
    val centre = cluster?.geometry() as? Point
    if (centre != null) {
        val zoom = (map.cameraPosition.zoom + 2.0).coerceAtMost(CLUSTER_MAX_ZOOM + 1.0)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centre.latitude(), centre.longitude()), zoom))
    }
    onSite(null)
}

/** Forwards the screen's lifecycle to the map, which needs it to pause rendering and free memory. */
@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Walk the map back down from wherever the lifecycle currently is.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }
}
