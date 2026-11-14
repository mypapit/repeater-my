package net.mypapit.mobile.myrepeater.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.ui.DateFormats
import net.mypapit.mobile.myrepeater.ui.UserPosition
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import net.mypapit.mobile.myrepeater.ui.theme.OffAirRed
import net.mypapit.mobile.myrepeater.ui.theme.OnAirGreen

/** Everything the source lists about one repeater, laid out for programming a radio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterDetailScreen(
    repeater: Repeater,
    position: UserPosition?,
    heading: () -> Float?,
    onBack: () -> Unit,
    onShowOnMap: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repeater.callsign) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = { ReportMenu(repeater) },
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
            FrequencyCard(repeater)
            LocationCard(repeater, position, onShowOnMap)
            DirectionCard(repeater, position, heading)
            AboutCard(repeater)
            if (repeater.notes.isNotBlank()) {
                DetailCard(stringResource(R.string.detail_notes)) {
                    Text(repeater.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * The app bar menu. "Report incorrect info" opens the phone's e-mail app on a
 * draft holding this repeater's recorded details; the user edits and sends it.
 */
@Composable
private fun ReportMenu(repeater: Repeater) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.detail_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_report)) },
                onClick = {
                    open = false
                    if (!CorrectionEmail.open(context, repeater)) {
                        Toast.makeText(context, R.string.detail_report_no_app, Toast.LENGTH_LONG).show()
                    }
                },
            )
        }
    }
}

@Composable
private fun FrequencyCard(repeater: Repeater) {
    DetailCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(repeater.status)
            Spacer(Modifier.weight(1f))
            Text(
                text = listOf(repeater.band, repeater.mode).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.detail_listen_on),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = repeater.downlinkMhz?.let { RepeaterFormat.frequency(it) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.unit_mhz),
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Field(
            stringResource(R.string.detail_transmit_on),
            repeater.uplinkMhz?.let { stringResource(R.string.value_mhz, RepeaterFormat.frequency(it)) },
        )
        Field(
            stringResource(R.string.detail_offset),
            repeater.offsetMhz?.let { stringResource(R.string.value_mhz, RepeaterFormat.offset(it)) },
        )
        Field(
            stringResource(R.string.detail_tone),
            repeater.tone.ifBlank { stringResource(R.string.detail_tone_none) },
        )
    }
}

@Composable
private fun LocationCard(repeater: Repeater, position: UserPosition?, onShowOnMap: () -> Unit) {
    DetailCard(stringResource(R.string.detail_location)) {
        Text(
            text = listOf(repeater.location, repeater.state).filter { it.isNotBlank() }.joinToString(", ")
                .ifBlank { stringResource(R.string.detail_unknown) },
            style = MaterialTheme.typography.titleMedium,
        )
        val lat = repeater.latitude
        val lon = repeater.longitude
        if (lat == null || lon == null) {
            Text(
                text = stringResource(R.string.detail_no_coordinates),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (position != null) {
                Text(
                    text = stringResource(
                        if (position.source == LocationMode.DEVICE) R.string.detail_distance
                        else R.string.detail_distance_approx,
                        RepeaterFormat.distance(Geo.distanceKm(position.point, GeoPoint(lat, lon))),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.detail_coordinates_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onShowOnMap) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_show_on_map))
            }
        }
    }
}

/**
 * The compass, shown only when measuring from the phone's own position. Within
 * [Geo.COMPASS_MIN_DISTANCE_KM] it is replaced by an explanation, because the
 * listed coordinates are too coarse to point with at short range.
 */
@Composable
private fun DirectionCard(repeater: Repeater, position: UserPosition?, heading: () -> Float?) {
    val lat = repeater.latitude ?: return
    val lon = repeater.longitude ?: return
    if (position == null || position.source != LocationMode.DEVICE) return

    val target = GeoPoint(lat, lon)
    val distance = Geo.distanceKm(position.point, target)
    val bearing = Geo.bearingDeg(position.point, target)
    // Derived so that a turning phone redraws the dial without recomposing this card.
    val hasHeading by remember(heading) { derivedStateOf { heading() != null } }

    DetailCard(stringResource(R.string.detail_direction)) {
        if (distance <= Geo.COMPASS_MIN_DISTANCE_KM) {
            Text(
                text = stringResource(R.string.detail_compass_near),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@DetailCard
        }
        Text(
            text = stringResource(
                R.string.detail_bearing,
                bearing.roundToInt() % 360,
                Geo.compassPoint(bearing),
                RepeaterFormat.distance(distance),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        if (hasHeading) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CompassDial(bearing = bearing.toFloat(), heading = heading)
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = stringResource(
                if (hasHeading) R.string.detail_compass_note else R.string.detail_compass_unavailable,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutCard(repeater: Repeater) {
    DetailCard(stringResource(R.string.detail_about)) {
        Field(
            stringResource(R.string.detail_club),
            repeater.clubFullName.ifBlank { repeater.club },
            first = true,
        )
        Field(stringResource(R.string.detail_network), repeater.linkedNetwork.ifBlank { null })
        Field(
            stringResource(R.string.detail_last_reviewed),
            repeater.lastReviewed.ifBlank { null }?.let { DateFormats.fromMonthFirst(it) },
        )
        Field(stringResource(R.string.detail_source), repeater.source.ifBlank { null })
    }
}

@Composable
private fun DetailCard(title: String?, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

/** A label and its value on one line. Rows without a value are left out. */
@Composable
private fun Field(label: String, value: String?, first: Boolean = false) {
    if (value == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (first) 0.dp else 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusPill(status: RepeaterStatus) {
    val color = when (status) {
        RepeaterStatus.ON_AIR -> OnAirGreen
        RepeaterStatus.OFF_AIR -> OffAirRed
        RepeaterStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                when (status) {
                    RepeaterStatus.ON_AIR -> R.string.status_on_air
                    RepeaterStatus.OFF_AIR -> R.string.status_off_air
                    RepeaterStatus.UNKNOWN -> R.string.status_unknown
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
