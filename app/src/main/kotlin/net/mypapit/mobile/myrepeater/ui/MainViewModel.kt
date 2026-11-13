package net.mypapit.mobile.myrepeater.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mypapit.mobile.myrepeater.AppContainer
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.sync.SyncState

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * True once a full sync has finished and there are repeaters to show;
     * null while that is still being read from disk.
     */
    val dataReady: StateFlow<Boolean?> = combine(
        container.syncPrefs.lastSync,
        container.database.repeaterDao().observeCount(),
    ) { lastSync, count -> lastSync != null && count > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val repeaters: StateFlow<List<Repeater>> = container.database.repeaterDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
}
