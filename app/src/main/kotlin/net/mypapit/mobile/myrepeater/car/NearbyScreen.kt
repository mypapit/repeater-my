package net.mypapit.mobile.myrepeater.car

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarSensors
import androidx.car.app.hardware.info.Compass
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import net.mypapit.mobile.myrepeater.AppContainer
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.location.LocationFix
import net.mypapit.mobile.myrepeater.data.location.LocationSource
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem

/** Where the car screen measures from, or why it cannot. */
private sealed interface Origin {
    /** Device location is chosen but not allowed, or no place has been picked. */
    data object Unavailable : Origin

    /** Allowed, and waiting for the first fix. */
    data object Waiting : Origin

    data class At(val fix: LocationFix, val placeName: String? = null) : Origin
}

private sealed interface CarState {
    data object Loading : CarState
    data object NoData : CarState
    data object NeedLocation : CarState
    data class Ready(val view: DirectionView) : CarState
}

/**
 * The one screen of the car app: the five nearest repeaters in a list, with an
 * interactive map and compact direction HUD beside it.
 *
 * Location is read only while this screen is showing, and is used on the
 * device to order the list and aim the arrow. Nothing is sent anywhere.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NearbyScreen(
    carContext: CarContext,
    private val container: AppContainer,
    private val renderer: CarMapSurface,
) : Screen(carContext) {

    private val permission = MutableStateFlow(container.locationSource.hasPermission())
    private val focusRequest = MutableStateFlow(CarMemory.focusedRepeaterId)
    private val handler = Handler(Looper.getMainLooper())
    private val templates = NearbyTemplates(carContext)
    private val mapActions = CarMapActions(
        onZoomIn = renderer::zoomIn,
        onZoomOut = renderer::zoomOut,
        onRecenter = renderer::recenter,
        onPanModeChanged = renderer::setPanMode,
    )

    private var state: CarState = CarState.Loading

    /** The order last worked out, and where the car was then. */
    private var ordered: List<RepeaterListItem> = emptyList()
    private var orderedAt: GeoPoint? = null
    private var orderedFrom: List<Repeater>? = null

    /** Row titles last sent to the car, or null if the last template sent was not a filled list. */
    private var sentTitles: List<String>? = null
    private var lastShown: Any? = null

    init {
        renderer.setCallbacks(
            onZoomChanged = { zoom ->
                lifecycleScope.launch { container.appSettings.setCarMapZoom(zoom) }
            },
            onRepeaterTapped = { id -> focusOn(id, fromMapTap = true) },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.appSettings.settings
                    .map { it.carMapZoom }
                    .distinctUntilChanged()
                    .collect(renderer::setPreferredZoom)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                permission.value = container.locationSource.hasPermission()
                states().collect(::show)
            }
        }
    }

    private fun states(): Flow<CarState> {
        val dataReady = combine(
            container.syncPrefs.lastSync,
            container.database.repeaterDao().observeCount(),
        ) { lastSync, count -> lastSync != null && count > 0 }

        val origin: Flow<Origin> = combine(container.appSettings.settings, permission) { settings, granted ->
            Triple(settings.locationMode, settings.presetKey, granted)
        }.distinctUntilChanged().flatMapLatest { (mode, presetKey, granted) ->
            when {
                mode == LocationMode.PRESET -> flowOf(
                    container.presets.firstOrNull { it.key == presetKey }
                        ?.let { Origin.At(LocationFix(it.point), it.name) }
                        ?: Origin.Unavailable,
                )
                granted -> flow {
                    emit(Origin.Waiting)
                    emitAll(container.locationSource.fixes().map { Origin.At(it) })
                }
                else -> flowOf(Origin.Unavailable)
            }
        }

        return combine(
            dataReady,
            container.database.repeaterDao().observeAll(),
            origin,
            carCompass(),
            focusRequest,
        ) { ready, repeaters, where, compass, focus ->
            when {
                !ready -> CarState.NoData
                where is Origin.Unavailable -> CarState.NeedLocation
                where is Origin.At -> CarState.Ready(view(repeaters, where, compass, focus))
                else -> CarState.Loading
            }
        }
    }

    private fun view(repeaters: List<Repeater>, where: Origin.At, compass: Float?, focus: Long?): DirectionView {
        val point = where.fix.point
        if (orderedFrom !== repeaters || NearbyRepeaters.shouldReorder(orderedAt, point)) {
            ordered = NearbyRepeaters.select(repeaters, point, rowLimit())
            orderedAt = point
            orderedFrom = repeaters
        } else {
            ordered = NearbyRepeaters.remeasure(ordered, point)
        }
        val activeWithCoords = repeaters.filter { it.status != RepeaterStatus.OFF_AIR && it.hasCoordinates }
        return DirectionView(
            origin = point,
            placeName = where.placeName,
            items = ordered,
            focusedId = NearbyRepeaters.resolveFocus(focus, ordered, activeWithCoords),
            // A chosen place is not where the car is, so the car's heading says nothing about it.
            heading = if (where.placeName != null) null else Headings.pick(compass, where.fix.courseDeg, where.fix.speedMps),
            allRepeaters = activeWithCoords,
        )
    }

    /** How many rows this car allows, in case it is fewer than five. */
    private fun rowLimit(): Int = try {
        carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
    } catch (e: Exception) {
        NearbyRepeaters.MAX_ROWS
    }

    /**
     * The vehicle's compass heading, at most once a second; null where the car
     * has none or does not share it. Not every car does.
     */
    private fun carCompass(): Flow<Float?> = callbackFlow<Float?> {
        trySend(null)
        val sensors = try {
            carContext.getCarService(CarHardwareManager::class.java).carSensors
        } catch (e: Exception) {
            null
        }
        val listener = OnCarDataAvailableListener<Compass> { data ->
            val orientations = data.orientations
            val azimuth = if (orientations.status == CarValue.STATUS_SUCCESS) orientations.value?.firstOrNull() else null
            trySend(azimuth?.let { Headings.step(it.toDouble()).toFloat() })
        }
        try {
            sensors?.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, ContextCompat.getMainExecutor(carContext), listener)
        } catch (e: Exception) {
            // No permission or no sensor: carry on without a car compass.
        }
        awaitClose {
            try {
                sensors?.removeCompassListener(listener)
            } catch (e: Exception) {
                // Already gone with the car connection.
            }
        }
    }.distinctUntilChanged().sample(COMPASS_SAMPLE_MS)

    private fun show(next: CarState) {
        state = next
        renderer.show((next as? CarState.Ready)?.view)

        // The car is asked to redraw the list only when its words or selection have changed.
        val shown: Any = when (next) {
            is CarState.Ready -> next.view.items.map(::rowKey) to next.view.focusedId
            else -> next
        }
        if (shown != lastShown) {
            lastShown = shown
            invalidate()
        }
    }

    private fun rowKey(item: RepeaterListItem): List<String> =
        listOf(CarRowText.title(item.repeater), templates.tuning(item), CarRowText.whereabouts(item))

    override fun onGetTemplate(): Template = when (val current = state) {
        CarState.Loading -> loading(withMapControls = false)
        CarState.NoData -> templates.noData().also { sentTitles = null }
        CarState.NeedLocation -> templates.needLocation {
            carContext.requestPermissions(LocationSource.PERMISSIONS.toList()) { _, _ ->
                permission.value = container.locationSource.hasPermission()
            }
        }.also { sentTitles = null }
        is CarState.Ready -> list(current.view)
    }

    private fun loading(withMapControls: Boolean): Template {
        sentTitles = null
        return templates.loading(if (withMapControls) mapActions else null)
    }

    private fun list(view: DirectionView): Template {
        val titles = view.items.map { CarRowText.title(it.repeater) }

        // The car counts a list with different row titles as a new step, and allows only five
        // steps. A list that follows a "loading" one is free, so a changed list is sent in two
        // parts: loading now, the new rows a moment later.
        val previous = sentTitles
        if (previous != null && previous != titles) {
            handler.post(::invalidate)
            return loading(withMapControls = true)
        }
        sentTitles = titles
        return templates.list(
            items = view.items,
            focusedId = view.focusedId,
            onFocus = { id -> focusOn(id, fromMapTap = false) },
            mapActions = mapActions,
        )
    }

    private fun focusOn(repeaterId: Long, fromMapTap: Boolean) {
        CarMemory.focusedRepeaterId = repeaterId
        focusRequest.value = repeaterId
        if (!fromMapTap) {
            renderer.focusRepeater(repeaterId)
        }
    }

    private companion object {
        const val COMPASS_SAMPLE_MS = 1_000L
    }
}

/** Remembers the driver's choice for as long as the app's process lives, so reopening the app restores it. */
internal object CarMemory {
    @Volatile
    var focusedRepeaterId: Long? = null
}
