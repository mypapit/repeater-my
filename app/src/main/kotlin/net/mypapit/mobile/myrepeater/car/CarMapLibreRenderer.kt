package net.mypapit.mobile.myrepeater.car

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.Gravity
import android.widget.FrameLayout
import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.min

/**
 * Interactive MapLibre renderer projected onto the Android Auto surface via a
 * [VirtualDisplay] and [Presentation], with a compact corner HUD overlay.
 *
 * Uses OpenFreeMap day/night vector tiles (shared with the phone app's cache)
 * and falls back automatically to bundled offline styles when offline. If a head
 * unit or device refuses VirtualDisplay creation, it seamlessly falls back to
 * [DirectionRenderer] so the car map is never blank.
 */
class CarMapLibreRenderer(
    private val carContext: CarContext,
    lifecycle: Lifecycle,
) : CarMapSurface, DefaultLifecycleObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val fallback = DirectionRenderer(carContext)
    private var usingFallback = false
    private var released = false

    private var surfaceContainer: SurfaceContainer? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarMapPresentation? = null
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var hudView: CarMapHudView? = null

    private var width = 0
    private var height = 0
    private var dpi = 160
    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null

    private var view: DirectionView? = null
    private var dark = true
    private var preferredZoom = CarMapZoom.DEFAULT_ZOOM
    private var currentZoom = CarMapZoom.DEFAULT_ZOOM
    private var panMode = false
    private var userPanned = false
    private var initialCameraPlaced = false
    private var onFallbackStyle = false
    private var styleToken = 0
    private var lastLabelKey: String? = null

    private var styleTimeoutRunnable: Runnable? = null
    private var zoomPersistRunnable: Runnable? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var onZoomChanged: ((Double) -> Unit)? = null
    private var onRepeaterTapped: ((Long) -> Unit)? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun show(view: DirectionView?) {
        this.view = view
        if (usingFallback) {
            fallback.show(view)
            return
        }
        syncSources()
        updateHud()
        if (view != null && (!initialCameraPlaced || !userPanned)) {
            moveToFocus(explicitFocus = false, animate = initialCameraPlaced)
            initialCameraPlaced = true
        }
    }

    override fun setDark(dark: Boolean) {
        if (this.dark == dark) return
        this.dark = dark
        if (usingFallback) {
            fallback.setDark(dark)
            return
        }
        lastLabelKey = null
        updateHud()
        if (map != null) loadStyle()
    }

    override fun setPreferredZoom(zoom: Double) {
        val clamped = CarMapZoom.clampZoom(zoom)
        if (kotlin.math.abs(preferredZoom - clamped) < 0.01) return
        preferredZoom = clamped
        currentZoom = clamped
        if (usingFallback) {
            fallback.setPreferredZoom(clamped)
            return
        }
        val ready = map ?: return
        if (!userPanned) {
            val target = view?.origin ?: CarMapZoom.DEFAULT_CENTRE
            ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), clamped))
        } else {
            ready.moveCamera(CameraUpdateFactory.zoomTo(clamped))
        }
    }

    override fun setPanMode(enabled: Boolean) {
        panMode = enabled
        if (usingFallback) {
            fallback.setPanMode(enabled)
            return
        }
        updateHud()
    }

    override fun focusRepeater(repeaterId: Long) {
        if (usingFallback) {
            fallback.focusRepeater(repeaterId)
            return
        }
        val v = view ?: return
        val focused = v.copy(focusedId = repeaterId).focusedItem()
        val dist = focused?.distanceKm ?: 0.0
        val target = CarMapZoom.computeFocusTarget(
            origin = v.origin,
            focused = focused,
            preferredZoom = preferredZoom,
            userPanned = userPanned,
            explicitFocus = true,
        ) ?: return
        userPanned = dist > CarMapZoom.sameFrameKm(target.zoom)
        currentZoom = target.zoom
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.center.latitude, target.center.longitude), target.zoom),
            CAMERA_ANIM_MS,
        )
    }

    override fun zoomIn() {
        if (usingFallback) {
            fallback.zoomIn()
            return
        }
        stepZoom(CarMapZoom.ZOOM_STEP)
    }

    override fun zoomOut() {
        if (usingFallback) {
            fallback.zoomOut()
            return
        }
        stepZoom(-CarMapZoom.ZOOM_STEP)
    }

    override fun recenter() {
        if (usingFallback) {
            fallback.recenter()
            return
        }
        userPanned = false
        currentZoom = preferredZoom
        val origin = view?.origin ?: return
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(origin.latitude, origin.longitude), preferredZoom),
            CAMERA_ANIM_MS,
        )
    }

    override fun setCallbacks(onZoomChanged: (Double) -> Unit, onRepeaterTapped: (Long) -> Unit) {
        this.onZoomChanged = onZoomChanged
        this.onRepeaterTapped = onRepeaterTapped
        fallback.setCallbacks(onZoomChanged, onRepeaterTapped)
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        if (released) return
        this.surfaceContainer = surfaceContainer
        width = surfaceContainer.width
        height = surfaceContainer.height
        dpi = surfaceContainer.dpi.takeIf { it > 0 } ?: carContext.resources.configuration.densityDpi
        val surface = surfaceContainer.surface ?: return
        if (width <= 0 || height <= 0) return

        if (usingFallback) {
            fallback.onSurfaceAvailable(surfaceContainer)
            return
        }

        tearDownMapSurface()
        try {
            MapLibre.getInstance(carContext.applicationContext)
            val displayManager = carContext.getSystemService(DisplayManager::class.java)
            val vd = displayManager.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                dpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            )
            virtualDisplay = vd
            val display = vd?.display ?: throw IllegalStateException("VirtualDisplay returned null Display")

            val pres = CarMapPresentation(carContext, display, dark) { mv, hud ->
                mapView = mv
                hudView = hud
                hud.setSafeArea(effectiveArea())
                updateHud()
                mv.getMapAsync { ready ->
                    if (released || presentation == null) return@getMapAsync
                    map = ready
                    configureMap(ready)
                    loadStyle()
                }
            }
            presentation = pres
            pres.show()
            registerConnectivityCallback()
        } catch (t: Throwable) {
            activateFallback(surfaceContainer)
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = Rect(visibleArea)
        if (usingFallback) {
            fallback.onVisibleAreaChanged(visibleArea)
            return
        }
        applySafeInsets()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = Rect(stableArea)
        if (usingFallback) {
            fallback.onStableAreaChanged(stableArea)
            return
        }
        applySafeInsets()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        this.surfaceContainer = null
        if (usingFallback) {
            fallback.onSurfaceDestroyed(surfaceContainer)
            return
        }
        tearDownMapSurface()
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        if (usingFallback) {
            fallback.onScroll(distanceX, distanceY)
            return
        }
        val ready = map ?: return
        if (!distanceX.isFinite() || !distanceY.isFinite()) return
        userPanned = true
        ready.scrollBy(-distanceX, -distanceY)
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        if (usingFallback) {
            fallback.onFling(velocityX, velocityY)
            return
        }
        val ready = map ?: return
        if (!velocityX.isFinite() || !velocityY.isFinite()) return
        userPanned = true
        val maxStep = (min(width, height).coerceAtLeast(200) * 0.4f)
        val dx = (velocityX * FLING_FACTOR).coerceIn(-maxStep, maxStep)
        val dy = (velocityY * FLING_FACTOR).coerceIn(-maxStep, maxStep)
        ready.scrollBy(dx, dy, FLING_DURATION_MS)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if (usingFallback) {
            fallback.onScale(focusX, focusY, scaleFactor)
            return
        }
        val ready = map ?: return
        val delta = CarMapZoom.zoomDeltaFromScale(scaleFactor)
        if (delta == 0.0) return
        val baseZoom = ready.cameraPosition.zoom
        val newZoom = CarMapZoom.clampZoom(baseZoom + delta)
        val fx = if (focusX.isFinite()) focusX else width / 2f
        val fy = if (focusY.isFinite()) focusY else height / 2f
        ready.setZoom(newZoom, PointF(fx, fy), 0L)
        currentZoom = newZoom
        preferredZoom = newZoom
        scheduleZoomPersist(newZoom)
    }

    override fun onClick(x: Float, y: Float) {
        if (usingFallback) {
            fallback.onClick(x, y)
            return
        }
        val ready = map ?: return
        val v = view ?: return
        if (!x.isFinite() || !y.isFinite()) return

        val nearbyIds = v.items.map { it.repeater.id }.toSet()
        val all = v.allRepeaters.ifEmpty { v.items.map { it.repeater } }
        val candidates = all.asSequence()
            .filter { it.hasCoordinates }
            .map { r ->
                val screen = ready.projection.toScreenLocation(LatLng(r.latitude!!, r.longitude!!))
                r to (screen.x to screen.y)
            }
            .toList()
        val maxTapPx = TAP_RADIUS_DP * (dpi.coerceAtLeast(120) / 160f)
        val hit = CarMapZoom.hitTestRepeater(x, y, candidates, nearbyIds, maxTapPx) ?: return
        onRepeaterTapped?.invoke(hit.id)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!usingFallback) mapView?.onStart()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!usingFallback) mapView?.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!usingFallback) mapView?.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!usingFallback) mapView?.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    override fun release() {
        released = true
        zoomPersistRunnable?.let(mainHandler::removeCallbacks)
        zoomPersistRunnable = null
        tearDownMapSurface()
        fallback.release()
    }

    private fun activateFallback(container: SurfaceContainer) {
        usingFallback = true
        tearDownMapSurface()
        fallback.setOutline(MapOutline.load(carContext))
        fallback.setDark(dark)
        fallback.setPreferredZoom(preferredZoom)
        fallback.setCallbacks(
            onZoomChanged = { onZoomChanged?.invoke(it) },
            onRepeaterTapped = { onRepeaterTapped?.invoke(it) },
        )
        visibleArea?.let(fallback::onVisibleAreaChanged)
        stableArea?.let(fallback::onStableAreaChanged)
        fallback.show(view)
        fallback.onSurfaceAvailable(container)
    }

    private fun configureMap(ready: MapLibreMap) {
        ready.setMinZoomPreference(CarMapZoom.MIN_ZOOM)
        ready.setMaxZoomPreference(CarMapZoom.MAX_ZOOM)
        val margin = (8f * (dpi.coerceAtLeast(120) / 160f)).toInt()
        ready.uiSettings.apply {
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isLogoEnabled = false
            isCompassEnabled = false
            attributionGravity = Gravity.BOTTOM or Gravity.END
            setAttributionMargins(margin, margin, margin, margin)
        }
        applySafeInsets()
        val target = view?.origin ?: CarMapZoom.DEFAULT_CENTRE
        val zoom = if (view != null) preferredZoom else CarMapZoom.OVERVIEW_ZOOM
        ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), zoom))
        initialCameraPlaced = view != null
    }

    private fun loadStyle() {
        val ready = map ?: return
        styleToken++
        val token = styleToken
        styleTimeoutRunnable?.let(mainHandler::removeCallbacks)
        style = null
        onFallbackStyle = false
        lastLabelKey = null

        var loaded = false
        val onlineUri = if (dark) ONLINE_STYLE_DARK else ONLINE_STYLE_LIGHT
        val offlineUri = if (dark) OFFLINE_STYLE_DARK else OFFLINE_STYLE_LIGHT

        ready.setStyle(Style.Builder().fromUri(onlineUri)) { loadedStyle ->
            if (released || presentation == null || token != styleToken) return@setStyle
            loaded = true
            onFallbackStyle = false
            style = loadedStyle
            addMapLayers(loadedStyle)
            syncSources()
            updateHud()
        }

        val timeout = Runnable {
            if (released || presentation == null || token != styleToken || loaded) return@Runnable
            onFallbackStyle = true
            updateHud()
            ready.setStyle(Style.Builder().fromUri(offlineUri)) { fallbackStyle ->
                if (released || presentation == null || token != styleToken) return@setStyle
                style = fallbackStyle
                addMapLayers(fallbackStyle)
                syncSources()
                updateHud()
            }
        }
        styleTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, STYLE_TIMEOUT_MS)
    }

    private fun addMapLayers(s: Style) {
        if (s.getSource(SOURCE_CAR_ALL) != null) return

        val accentHex = if (dark) COLOR_ACCENT_DARK else COLOR_ACCENT_LIGHT
        val outlineHex = if (dark) COLOR_OUTLINE_DARK else COLOR_OUTLINE_LIGHT
        val band2m = if (dark) COLOR_2M_DARK else COLOR_2M_LIGHT
        val band70cm = if (dark) COLOR_70CM_DARK else COLOR_70CM_LIGHT
        val bandOther = if (dark) COLOR_MIXED_DARK else COLOR_MIXED_LIGHT
        val carHex = if (dark) COLOR_CAR_DARK else COLOR_CAR_LIGHT

        s.addSource(GeoJsonSource(SOURCE_CAR_LINE, FeatureCollection.fromFeatures(emptyList())))
        s.addSource(GeoJsonSource(SOURCE_CAR_ALL, FeatureCollection.fromFeatures(emptyList())))
        s.addSource(GeoJsonSource(SOURCE_CAR_NEARBY, FeatureCollection.fromFeatures(emptyList())))
        s.addSource(GeoJsonSource(SOURCE_CAR_FOCUSED, FeatureCollection.fromFeatures(emptyList())))
        s.addSource(GeoJsonSource(SOURCE_CAR_ORIGIN, FeatureCollection.fromFeatures(emptyList())))

        s.addLayer(
            LineLayer(LAYER_CAR_LINE, SOURCE_CAR_LINE).withProperties(
                lineColor(accentHex),
                lineWidth(3.5f),
                lineOpacity(0.9f),
            ),
        )

        val bandColorExpr = switchCase(
            eq(get(PROP_BAND), literal("2m")),
            literal(band2m),
            eq(get(PROP_BAND), literal("70cm")),
            literal(band70cm),
            literal(bandOther),
        )

        s.addLayer(
            CircleLayer(LAYER_CAR_ALL, SOURCE_CAR_ALL).withProperties(
                circleColor(bandColorExpr),
                circleRadius(5.5f),
                circleOpacity(0.70f),
                circleStrokeColor(outlineHex.toColorInt()),
                circleStrokeWidth(1.5f),
            ),
        )

        s.addLayer(
            CircleLayer(LAYER_CAR_NEARBY, SOURCE_CAR_NEARBY).withProperties(
                circleColor(bandColorExpr),
                circleRadius(8.5f),
                circleOpacity(1.0f),
                circleStrokeColor(outlineHex.toColorInt()),
                circleStrokeWidth(2.2f),
            ),
        )

        s.addLayer(
            CircleLayer(LAYER_CAR_FOCUSED_HALO, SOURCE_CAR_FOCUSED).withProperties(
                circleColor(accentHex.toColorInt()),
                circleRadius(16f),
                circleOpacity(0.32f),
            ),
        )

        s.addLayer(
            CircleLayer(LAYER_CAR_FOCUSED, SOURCE_CAR_FOCUSED).withProperties(
                circleColor(accentHex.toColorInt()),
                circleRadius(10.5f),
                circleStrokeColor(outlineHex.toColorInt()),
                circleStrokeWidth(2.5f),
            ),
        )

        s.addLayer(
            SymbolLayer(LAYER_CAR_FOCUSED_LABEL, SOURCE_CAR_FOCUSED).withProperties(
                iconImage(FOCUSED_LABEL_IMAGE),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconOffset(arrayOf(0f, -14f)),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ),
        )

        s.addLayer(
            CircleLayer(LAYER_CAR_ORIGIN, SOURCE_CAR_ORIGIN).withProperties(
                circleColor(carHex.toColorInt()),
                circleRadius(8f),
                circleStrokeColor(Color.WHITE),
                circleStrokeWidth(3f),
            ),
        )
    }

    private fun syncSources() {
        val s = style ?: return
        val v = view
        if (v == null) {
            s.getSourceAs<GeoJsonSource>(SOURCE_CAR_LINE)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            s.getSourceAs<GeoJsonSource>(SOURCE_CAR_ALL)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            s.getSourceAs<GeoJsonSource>(SOURCE_CAR_NEARBY)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            s.getSourceAs<GeoJsonSource>(SOURCE_CAR_FOCUSED)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            s.getSourceAs<GeoJsonSource>(SOURCE_CAR_ORIGIN)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }

        val focused = v.focusedItem()
        val nearbyIds = v.items.map { it.repeater.id }.toSet()

        val lineFeatures = if (focused?.repeater?.hasCoordinates == true) {
            listOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        listOf(
                            Point.fromLngLat(v.origin.longitude, v.origin.latitude),
                            Point.fromLngLat(focused.repeater.longitude!!, focused.repeater.latitude!!),
                        ),
                    ),
                ),
            )
        } else {
            emptyList()
        }
        s.getSourceAs<GeoJsonSource>(SOURCE_CAR_LINE)?.setGeoJson(FeatureCollection.fromFeatures(lineFeatures))

        val allPoints = v.allRepeaters.ifEmpty { v.items.map { it.repeater } }
        val otherFeatures = allPoints.mapNotNull { r ->
            if (!r.hasCoordinates || r.id in nearbyIds || r.id == focused?.repeater?.id) return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(r.longitude!!, r.latitude!!)).apply {
                addStringProperty(PROP_BAND, r.band)
            }
        }
        s.getSourceAs<GeoJsonSource>(SOURCE_CAR_ALL)?.setGeoJson(FeatureCollection.fromFeatures(otherFeatures))

        val nearbyFeatures = v.items.mapNotNull { item ->
            val r = item.repeater
            if (!r.hasCoordinates || r.id == focused?.repeater?.id) return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(r.longitude!!, r.latitude!!)).apply {
                addStringProperty(PROP_BAND, r.band)
            }
        }
        s.getSourceAs<GeoJsonSource>(SOURCE_CAR_NEARBY)?.setGeoJson(FeatureCollection.fromFeatures(nearbyFeatures))

        val focusedFeatures = if (focused?.repeater?.hasCoordinates == true) {
            val r = focused.repeater
            val labelKey = "${r.callsign}:$dark"
            if (labelKey != lastLabelKey) {
                lastLabelKey = labelKey
                val bmp = buildCallsignLabelBitmap(r.callsign, dark, dpi.coerceAtLeast(120) / 160f)
                s.addImage(FOCUSED_LABEL_IMAGE, bmp)
            }
            listOf(Feature.fromGeometry(Point.fromLngLat(r.longitude!!, r.latitude!!)))
        } else {
            emptyList()
        }
        s.getSourceAs<GeoJsonSource>(SOURCE_CAR_FOCUSED)?.setGeoJson(FeatureCollection.fromFeatures(focusedFeatures))

        val originFeatures = listOf(Feature.fromGeometry(Point.fromLngLat(v.origin.longitude, v.origin.latitude)))
        s.getSourceAs<GeoJsonSource>(SOURCE_CAR_ORIGIN)?.setGeoJson(FeatureCollection.fromFeatures(originFeatures))
    }

    private fun moveToFocus(explicitFocus: Boolean, animate: Boolean) {
        val ready = map ?: return
        val v = view ?: return
        val target = CarMapZoom.computeFocusTarget(
            origin = v.origin,
            focused = v.focusedItem(),
            preferredZoom = preferredZoom,
            userPanned = userPanned,
            explicitFocus = explicitFocus,
        ) ?: return
        currentZoom = target.zoom
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(target.center.latitude, target.center.longitude), target.zoom)
        if (animate) ready.animateCamera(update, CAMERA_ANIM_MS) else ready.moveCamera(update)
    }

    private fun stepZoom(delta: Double) {
        val ready = map ?: return
        val next = CarMapZoom.clampZoom(ready.cameraPosition.zoom + delta)
        currentZoom = next
        preferredZoom = next
        ready.animateCamera(CameraUpdateFactory.zoomTo(next), ZOOM_ANIM_MS)
        zoomPersistRunnable?.let(mainHandler::removeCallbacks)
        onZoomChanged?.invoke(next)
    }

    private fun scheduleZoomPersist(zoom: Double) {
        zoomPersistRunnable?.let(mainHandler::removeCallbacks)
        val runnable = Runnable { onZoomChanged?.invoke(zoom) }
        zoomPersistRunnable = runnable
        mainHandler.postDelayed(runnable, ZOOM_PERSIST_DEBOUNCE_MS)
    }

    private fun effectiveArea(): Rect =
        stableArea?.takeIf { !it.isEmpty }
            ?: visibleArea?.takeIf { !it.isEmpty }
            ?: Rect(0, 0, width, height)

    private fun applySafeInsets() {
        val area = effectiveArea()
        hudView?.setSafeArea(area)
        val ready = map ?: return
        if (width <= 0 || height <= 0) return
        val left = area.left.coerceIn(0, width)
        val top = area.top.coerceIn(0, height)
        val right = (width - area.right).coerceIn(0, width)
        val bottom = (height - area.bottom).coerceIn(0, height)
        ready.moveCamera(
            CameraUpdateFactory.paddingTo(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble()),
        )
        val margin = (8f * (dpi.coerceAtLeast(120) / 160f)).toInt()
        ready.uiSettings.setAttributionMargins(left + margin, top + margin, right + margin, bottom + margin)
    }

    private fun updateHud() {
        hudView?.update(view = view, dark = dark, offline = onFallbackStyle, panMode = panMode)
    }

    private fun registerConnectivityCallback() {
        if (networkCallback != null) return
        val cm = carContext.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    if (!released && !usingFallback && onFallbackStyle && map != null) {
                        loadStyle()
                    }
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (_: Exception) {
        }
    }

    private fun tearDownMapSurface() {
        styleToken++
        styleTimeoutRunnable?.let(mainHandler::removeCallbacks)
        styleTimeoutRunnable = null

        networkCallback?.let { cb ->
            try {
                carContext.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb)
            } catch (_: Exception) {
            }
        }
        networkCallback = null

        mapView?.let { mv ->
            try {
                mv.onPause()
                mv.onStop()
                mv.onDestroy()
            } catch (_: Exception) {
            }
        }
        mapView = null
        map = null
        style = null
        hudView = null
        initialCameraPlaced = false

        presentation?.let { pres ->
            try {
                pres.dismiss()
            } catch (_: Exception) {
            }
        }
        presentation = null

        virtualDisplay?.let { vd ->
            try {
                vd.release()
            } catch (_: Exception) {
            }
        }
        virtualDisplay = null
    }

    private fun buildCallsignLabelBitmap(callsign: String, dark: Boolean, density: Float): Bitmap {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) Color.WHITE else Color.rgb(0x15, 0x1C, 0x24)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f * density
            textAlign = Paint.Align.CENTER
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) Color.argb(0xEB, 0x12, 0x1B, 0x26) else Color.argb(0xF5, 0xFF, 0xFF, 0xFF)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (if (dark) COLOR_ACCENT_DARK else COLOR_ACCENT_LIGHT).toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * density
        }
        val padH = 9f * density
        val padV = 5f * density
        val textW = textPaint.measureText(callsign)
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val w = (textW + padH * 2f).toInt().coerceAtLeast(24)
        val h = (textH + padV * 2f).toInt().coerceAtLeast(16)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val strokeHalf = borderPaint.strokeWidth / 2f
        val rect = RectF(strokeHalf, strokeHalf, w - strokeHalf, h - strokeHalf)
        val radius = 8f * density
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        val baseline = (h - textH) / 2f - fm.ascent
        canvas.drawText(callsign, w / 2f, baseline, textPaint)
        return bitmap
    }

    private class CarMapPresentation(
        outerContext: Context,
        display: Display,
        private val dark: Boolean,
        private val onReady: (MapView, CarMapHudView) -> Unit,
    ) : Presentation(outerContext, display) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val uiContext = ContextThemeWrapper(context, R.style.Theme_RepeaterMY)
            val options = MapLibreMapOptions.createFromAttributes(uiContext)
                .textureMode(true)
                .foregroundLoadColor((if (dark) COLOR_OUTLINE_DARK else COLOR_OUTLINE_LIGHT).toColorInt())
            val mv = MapView(uiContext, options)
            mv.onCreate(null)
            mv.onStart()
            mv.onResume()

            val hud = CarMapHudView(uiContext)
            val root = FrameLayout(uiContext).apply {
                addView(
                    mv,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    hud,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            setContentView(root)
            onReady(mv, hud)
        }
    }

    private companion object {
        const val VIRTUAL_DISPLAY_NAME = "RepeaterCarMap"
        const val ONLINE_STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"
        const val ONLINE_STYLE_LIGHT = "https://tiles.openfreemap.org/styles/bright"
        const val OFFLINE_STYLE_DARK = "asset://map_style_offline.json"
        const val OFFLINE_STYLE_LIGHT = "asset://map_style_offline_light.json"

        const val SOURCE_CAR_LINE = "car-bearing-line"
        const val SOURCE_CAR_ALL = "car-all-repeaters"
        const val SOURCE_CAR_NEARBY = "car-nearby-repeaters"
        const val SOURCE_CAR_FOCUSED = "car-focused-repeater"
        const val SOURCE_CAR_ORIGIN = "car-origin"

        const val LAYER_CAR_LINE = "car-bearing-line-layer"
        const val LAYER_CAR_ALL = "car-all-repeaters-layer"
        const val LAYER_CAR_NEARBY = "car-nearby-repeaters-layer"
        const val LAYER_CAR_FOCUSED_HALO = "car-focused-halo-layer"
        const val LAYER_CAR_FOCUSED = "car-focused-pin-layer"
        const val LAYER_CAR_FOCUSED_LABEL = "car-focused-label-layer"
        const val LAYER_CAR_ORIGIN = "car-origin-layer"

        const val PROP_BAND = "band"
        const val FOCUSED_LABEL_IMAGE = "car-focused-callsign"

        const val COLOR_OUTLINE_DARK = "#0E1620"
        const val COLOR_OUTLINE_LIGHT = "#FFFFFF"
        const val COLOR_ACCENT_DARK = "#FFC107"
        const val COLOR_ACCENT_LIGHT = "#C62828"
        const val COLOR_2M_DARK = "#FF8500"
        const val COLOR_2M_LIGHT = "#E65100"
        const val COLOR_70CM_DARK = "#33B5E5"
        const val COLOR_70CM_LIGHT = "#0277BD"
        const val COLOR_MIXED_DARK = "#C9A0FF"
        const val COLOR_MIXED_LIGHT = "#7E57C2"
        const val COLOR_CAR_DARK = "#4FC3F7"
        const val COLOR_CAR_LIGHT = "#1565C0"

        const val STYLE_TIMEOUT_MS = 4_000L
        const val ZOOM_PERSIST_DEBOUNCE_MS = 250L
        const val CAMERA_ANIM_MS = 350
        const val ZOOM_ANIM_MS = 220
        const val FLING_FACTOR = 0.12f
        const val FLING_DURATION_MS = 220L
        const val TAP_RADIUS_DP = 44f
    }
}
