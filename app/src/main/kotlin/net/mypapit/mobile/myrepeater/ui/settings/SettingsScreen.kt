package net.mypapit.mobile.myrepeater.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.BuildConfig
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.location.PresetLocation
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.settings.Settings
import net.mypapit.mobile.myrepeater.data.sync.SyncPolicy
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import kotlinx.coroutines.delay
import net.mypapit.mobile.myrepeater.ui.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    locationGranted: Boolean,
    presetName: String?,
    lastSync: Long?,
    lastSyncAttempt: Long?,
    syncState: SyncState,
    onBack: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onChoosePreset: () -> Unit,
    onSync: () -> Unit,
    onAprsChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.settings_location_section)) {
                val deviceSelected = settings.locationMode == LocationMode.DEVICE
                ChoiceRow(
                    selected = deviceSelected,
                    title = stringResource(R.string.settings_location_device),
                    subtitle = stringResource(
                        if (locationGranted) R.string.settings_location_device_on
                        else R.string.settings_location_device_needs_permission,
                    ),
                    onClick = {
                        onUseDeviceLocation()
                        if (!locationGranted) onRequestLocation()
                    },
                )
                ChoiceRow(
                    selected = !deviceSelected,
                    title = stringResource(R.string.settings_location_preset),
                    subtitle = presetName ?: stringResource(R.string.settings_location_preset_none),
                    onClick = onChoosePreset,
                )
                if (deviceSelected && !locationGranted) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_location_permission_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            TextButton(onClick = onRequestLocation) {
                                Text(stringResource(R.string.settings_location_allow))
                            }
                            TextButton(onClick = onOpenSystemSettings) {
                                Text(stringResource(R.string.settings_location_system))
                            }
                        }
                    }
                }
            }

            Section(stringResource(R.string.settings_data_section)) {
                val updating = syncState is SyncState.Downloading || syncState is SyncState.Processing
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = if (lastSync == null) {
                            stringResource(R.string.settings_data_never)
                        } else {
                            val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
                            val formatted = remember(lastSync, use24Hour) {
                                DateFormats.malaysiaDateTime(lastSync, use24Hour)
                            }
                            stringResource(R.string.settings_data_last_updated, formatted)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (syncState is SyncState.Failed) {
                        Text(
                            text = stringResource(R.string.settings_data_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (updating) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                    // The wait counts down while the screen is open, and the button comes back by itself.
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(CLOCK_TICK_MS)
                            now = System.currentTimeMillis()
                        }
                    }
                    val waitMs = if (lastSync == null) 0L else SyncPolicy.manualAllowedAt(lastSync, lastSyncAttempt, now) - now

                    FilledTonalButton(onClick = onSync, enabled = !updating && waitMs <= 0) {
                        Text(
                            stringResource(
                                if (updating) R.string.settings_data_updating else R.string.settings_data_update,
                            ),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (!updating && waitMs > 0) waitText(waitMs) else stringResource(R.string.settings_data_auto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Section(stringResource(R.string.settings_compass_section)) {
                Text(
                    text = stringResource(R.string.list_compass_disclaimer),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section(stringResource(R.string.settings_experimental_section)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = settings.aprsEnabled, role = Role.Switch, onValueChange = onAprsChange)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_aprs_title), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(R.string.settings_aprs_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = settings.aprsEnabled, onCheckedChange = null)
                }
            }

            Section(stringResource(R.string.settings_privacy_section)) {
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(
                onClick = onOpenAbout,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.about_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.settings_about_summary, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The place picker: every bundled location, grouped under its state. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PresetPickerScreen(
    presets: List<PresetLocation>,
    selectedKey: String?,
    onBack: () -> Unit,
    onPick: (PresetLocation) -> Unit,
) {
    val byState = remember(presets) { presets.groupBy { it.state }.toSortedMap() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.presets_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            byState.forEach { (state, places) ->
                stickyHeader(key = "state:$state") {
                    Text(
                        text = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(places, key = { it.key }) { place ->
                    ChoiceRow(
                        selected = place.key == selectedKey,
                        title = place.name,
                        subtitle = null,
                        onClick = { onPick(place) },
                    )
                }
            }
        }
    }
}

private const val CLOCK_TICK_MS = 20_000L
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS

/** "Available again in about 5 hours", rounded up so it never promises too early. */
@Composable
private fun waitText(waitMs: Long): String =
    if (waitMs > HOUR_MS) {
        val hours = ((waitMs + HOUR_MS - 1) / HOUR_MS).toInt()
        pluralStringResource(R.plurals.settings_data_wait_hours, hours, hours)
    } else {
        val minutes = ((waitMs + MINUTE_MS - 1) / MINUTE_MS).toInt().coerceAtLeast(1)
        pluralStringResource(R.plurals.settings_data_wait_minutes, minutes, minutes)
    }

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) { content() }
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
