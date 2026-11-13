package net.mypapit.mobile.myrepeater.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mypapit.mobile.myrepeater.AppContainer
import net.mypapit.mobile.myrepeater.data.location.CompassSource
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.location.PresetLocation
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.settings.Settings
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import net.mypapit.mobile.myrepeater.ui.list.RepeaterSorter

/** Where distances are measured from, and whether that is the phone itself or a chosen place. */
data class UserPosition(val point: GeoPoint, val source: LocationMode, val placeName: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Null until the saved settings have been read. */
    val settings: StateFlow<Settings?> = container.appSettings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastSync: StateFlow<Long?> = container.syncPrefs.lastSync
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * True once a full sync has finished and there are repeaters to show;
     * null while that is still being read from disk.
     */
    val dataReady: StateFlow<Boolean?> = combine(
        container.syncPrefs.lastSync,
        container.database.repeaterDao().observeCount(),
    ) { lastSync, count -> lastSync != null && count > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _locationPermission = MutableStateFlow(container.locationSource.hasPermission())
    val locationPermission: StateFlow<Boolean> = _locationPermission.asStateFlow()

    val presets: List<PresetLocation> get() = container.presets
    val compassAvailable: Boolean get() = container.compassSource.isAvailable

    /**
     * The device position is only observed while something on screen collects
     * this, so location stops when the app goes to the background.
     */
    val position: StateFlow<UserPosition?> = combine(
        container.appSettings.settings,
        _locationPermission,
    ) { settings, granted -> Triple(settings.locationMode, settings.presetKey, granted) }
        .distinctUntilChanged()
        .flatMapLatest { (mode, presetKey, granted) ->
            when {
                mode == LocationMode.PRESET -> flowOf(
                    container.presets.firstOrNull { it.key == presetKey }
                        ?.let { UserPosition(it.point, LocationMode.PRESET, it.name) },
                )
                granted -> flow {
                    emit(null)
                    emitAll(container.locationSource.updates().map { UserPosition(it, LocationMode.DEVICE) })
                }
                else -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val items: StateFlow<List<RepeaterListItem>> = combine(
        container.database.repeaterDao().observeAll(),
        position,
    ) { repeaters, position -> RepeaterSorter.arrange(repeaters, position?.point) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** True heading of the top of the screen, or null when there is no compass to show. */
    val heading: StateFlow<Float?> = position
        .map { it?.takeIf { p -> p.source == LocationMode.DEVICE }?.point }
        .distinctUntilChanged { old, new -> (old == null) == (new == null) }
        .flatMapLatest { point ->
            if (point == null || !container.compassSource.isAvailable) {
                flowOf(null)
            } else {
                val declination = CompassSource.declination(point)
                container.compassSource.magneticHeadings().map { (it + declination + 360f) % 360f }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

    private var syncJob: Job? = null

    init {
        sync()
    }

    /** Fetches fresh data. With data already on the device this is a quiet background refresh. */
    fun sync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            container.syncRepository.sync().collect { _syncState.value = it }
        }
    }

    /** Call after a permission prompt and whenever the app returns to the foreground. */
    fun refreshLocationPermission() {
        _locationPermission.value = container.locationSource.hasPermission()
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.appSettings.setOnboardingDone() }
    }

    fun useDeviceLocation() {
        viewModelScope.launch { container.appSettings.useDeviceLocation() }
    }

    fun usePreset(preset: PresetLocation) {
        viewModelScope.launch { container.appSettings.usePreset(preset.key) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
