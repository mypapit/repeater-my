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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import net.mypapit.mobile.myrepeater.BuildConfig
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.location.PresetLocation
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.settings.Settings
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    locationGranted: Boolean,
    presetName: String?,
    lastSync: Long?,
    syncState: SyncState,
    onBack: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onChoosePreset: () -> Unit,
    onSync: () -> Unit,
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
                                DateFormats.dateTime(lastSync, ZoneId.systemDefault(), use24Hour)
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
                    FilledTonalButton(onClick = onSync, enabled = !updating) {
                        Text(
                            stringResource(
                                if (updating) R.string.settings_data_updating else R.string.settings_data_update,
                            ),
                        )
                    }
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
