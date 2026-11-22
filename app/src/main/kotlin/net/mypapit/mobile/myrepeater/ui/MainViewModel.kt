package net.mypapit.mobile.myrepeater.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mypapit.mobile.myrepeater.AppContainer
import net.mypapit.mobile.myrepeater.data.aprs.AprsRepository
import net.mypapit.mobile.myrepeater.data.aprs.AprsSnapshot
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.CompassSource
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.location.PresetLocation
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.settings.Settings
import net.mypapit.mobile.myrepeater.data.sync.SyncPolicy
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.callsign.CallsignSearch
import net.mypapit.mobile.myrepeater.ui.callsign.CallsignSearchState
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFilter
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import net.mypapit.mobile.myrepeater.ui.list.RepeaterSorter

/** Where distances are measured from, and whether that is the phone itself or a chosen place. */
data class UserPosition(val point: GeoPoint, val source: LocationMode, val placeName: String? = null)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Null until the saved settings have been read. */
    val settings: StateFlow<Settings?> = container.appSettings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastSync: StateFlow<Long?> = container.syncPrefs.lastSync
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** When a download was last started, whether or not it worked. */
    val lastAttempt: StateFlow<Long?> = container.syncPrefs.lastAttempt
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

    /** Every repeater, unfiltered: what the map and the detail screen read. */
    val allRepeaters: StateFlow<List<Repeater>> = container.database.repeaterDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _filter = MutableStateFlow(RepeaterFilter())
    val filter: StateFlow<RepeaterFilter> = _filter.asStateFlow()

    /** The list tab's rows: filtered, then ordered nearest first when a position is known. */
    val items: StateFlow<List<RepeaterListItem>> = combine(
        container.database.repeaterDao().observeAll(),
        position,
        _filter,
    ) { repeaters, position, filter -> RepeaterSorter.arrange(repeaters.filter(filter::matches), position?.point) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _callsignQuery = MutableStateFlow("")
    val callsignQuery: StateFlow<String> = _callsignQuery.asStateFlow()

    /** Matches for the callsign lookup. The query never leaves the device. */
    val callsignResults: StateFlow<CallsignSearchState> = _callsignQuery
        .debounce(CALLSIGN_DEBOUNCE_MS)
        .mapLatest { raw ->
            val query = CallsignSearch.clean(raw)
            if (query.length < CallsignSearch.MIN_QUERY_LENGTH) {
                CallsignSearchState.TooShort
            } else {
                val rows = container.database.callsignDao().search(query, CallsignSearch.MAX_RESULTS + 1)
                CallsignSearchState.Results(
                    records = rows.take(CallsignSearch.MAX_RESULTS),
                    truncated = rows.size > CallsignSearch.MAX_RESULTS,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CallsignSearchState.TooShort)

    /**
     * APRS stations for the map, or null while the experimental feature is off.
     * Only collected by the map screen, so nothing is downloaded unless the map is
     * open; the repository then limits the server to one request per 30 minutes.
     */
    val aprs: StateFlow<AprsSnapshot?> = container.appSettings.settings
        .map { it.aprsEnabled }
        .distinctUntilChanged()
        .flatMapLatest { enabled ->
            if (!enabled) {
                flowOf<AprsSnapshot?>(null)
            } else {
                flow<AprsSnapshot?> {
                    while (true) {
                        emit(container.aprsRepository.load())
                        delay(AprsRepository.MAX_AGE_MS)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _mapFocus = MutableStateFlow<GeoPoint?>(null)

    /** A place the map should jump to and select, set by "Show on map". */
    val mapFocus: StateFlow<GeoPoint?> = _mapFocus.asStateFlow()

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
        // On start the server is asked only when there is nothing saved, or the copy is over a week old.
        viewModelScope.launch {
            val prefs = container.syncPrefs
            val saved = prefs.lastSync.first().takeIf { container.database.repeaterDao().count() > 0 }
            if (SyncPolicy.autoDue(saved, prefs.lastAttempt.first(), System.currentTimeMillis())) startSync()
        }
    }

    /**
     * The user asking for fresh data. Ignored while the last download is too
     * recent, so tapping again and again cannot flood the server. With nothing
     * saved yet there is no wait: the app is unusable until a download works.
     */
    fun sync() {
        val now = System.currentTimeMillis()
        if (dataReady.value == true && !SyncPolicy.manualAllowed(lastSync.value, lastAttempt.value, now)) return
        startSync()
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            container.syncPrefs.setLastAttempt(System.currentTimeMillis())
            container.syncRepository.sync().collect { _syncState.value = it }
        }
    }

    fun dismissListNotice() {
        viewModelScope.launch { container.appSettings.dismissListNotice() }
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

    fun setAprsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.appSettings.setAprsEnabled(enabled)
            if (!enabled) container.aprsRepository.clear()
        }
    }

    fun setCarMapZoom(zoom: Double) {
        viewModelScope.launch { container.appSettings.setCarMapZoom(zoom) }
    }

    fun setFilter(filter: RepeaterFilter) {
        _filter.value = filter
    }

    fun setCallsignQuery(query: String) {
        _callsignQuery.value = query
    }

    fun showOnMap(point: GeoPoint) {
        _mapFocus.value = point
    }

    fun clearMapFocus() {
        _mapFocus.value = null
    }

    private companion object {
        const val CALLSIGN_DEBOUNCE_MS = 200L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
