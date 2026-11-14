package net.mypapit.mobile.myrepeater.ui.map

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.aprs.AprsSnapshot
import net.mypapit.mobile.myrepeater.data.aprs.AprsStation
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.ui.DateFormats
import net.mypapit.mobile.myrepeater.ui.UserPosition
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import net.mypapit.mobile.myrepeater.ui.theme.AprsPink
import net.mypapit.mobile.myrepeater.ui.theme.MixedBandPurple
import net.mypapit.mobile.myrepeater.ui.theme.SignalOrange
import net.mypapit.mobile.myrepeater.ui.theme.SkyBlue

/** Map of every repeater with known coordinates. Tapping a pin opens an info card for its site. */
@Composable
fun MapScreen(
    repeaters: List<Repeater>,
    position: UserPosition?,
    aprs: AprsSnapshot?,
    focusRequest: GeoPoint?,
    onFocusHandled: () -> Unit,
    onOpenRepeater: (Long) -> Unit,
) {
    val sites = remember(repeaters) { MapSites.from(repeaters) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = remember(sites, selectedId) { sites.firstOrNull { it.id == selectedId } }
    var lastSelected by remember { mutableStateOf<MapSite?>(null) }
    LaunchedEffect(selected) { if (selected != null) lastSelected = selected }
    var basemapUnavailable by remember { mutableStateOf(false) }
    // A repeater site and an APRS station are never selected together.
    var selectedStation by remember { mutableStateOf<AprsStation?>(null) }
    var lastStation by remember { mutableStateOf<AprsStation?>(null) }
    LaunchedEffect(selectedStation) { if (selectedStation != null) lastStation = selectedStation }
    LaunchedEffect(aprs) { if (aprs == null) selectedStation = null }
    // Centre on wherever the user was when the map first opened; later moves do not yank the camera.
    val focus = remember { position?.point }
    var savedCamera by rememberSaveable { mutableStateOf<DoubleArray?>(null) }

    // "Show on map" from a repeater: select its site; the map view moves the camera.
    LaunchedEffect(focusRequest, sites) {
        val target = focusRequest ?: return@LaunchedEffect
        sites.firstOrNull { it.point == target }?.let { selectedId = it.id }
    }

    Box(Modifier.fillMaxSize()) {
        RepeaterMapView(
            sites = sites,
            aprsStations = aprs?.stations.orEmpty(),
            userPosition = position?.takeIf { it.source == LocationMode.DEVICE }?.point,
            focus = focus,
            savedCamera = savedCamera,
            flyTo = focusRequest,
            onFlown = onFocusHandled,
            onCameraIdle = { savedCamera = it },
            onSiteTapped = {
                selectedId = it?.id
                selectedStation = null
            },
            onAprsTapped = {
                selectedStation = it
                selectedId = null
            },
            onBasemapUnavailable = { basemapUnavailable = it },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Legend(showAprs = aprs != null)
            if (basemapUnavailable) {
                Chip { Text(stringResource(R.string.map_offline_notice), style = MaterialTheme.typography.labelMedium) }
            }
            if (aprs?.failed == true) {
                Chip {
                    Text(
                        text = stringResource(
                            if (aprs.stations.isEmpty()) R.string.map_aprs_unavailable else R.string.map_aprs_stale,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            // Keep showing the last site while the card slides away.
            (selected ?: lastSelected)?.let { site ->
                SiteCard(site, position, onOpenRepeater, onClose = { selectedId = null })
            }
        }

        AnimatedVisibility(
            visible = selectedStation != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            (selectedStation ?: lastStation)?.let { station ->
                AprsCard(station, position, onClose = { selectedStation = null })
            }
        }
    }
}

@Composable
private fun Legend(showAprs: Boolean) {
    Chip {
        LegendDot(SignalOrange, stringResource(R.string.map_legend_2m))
        Spacer(Modifier.width(12.dp))
        LegendDot(SkyBlue, stringResource(R.string.map_legend_70cm))
        Spacer(Modifier.width(12.dp))
        LegendDot(MixedBandPurple, stringResource(R.string.map_legend_mixed))
        if (showAprs) {
            Spacer(Modifier.width(12.dp))
            LegendDot(AprsPink, stringResource(R.string.map_legend_aprs))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun Chip(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

/** Info window for an APRS station: who it is, when it was last heard, and what it sent. */
@Composable
private fun AprsCard(station: AprsStation, position: UserPosition?, onClose: () -> Unit) {
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(AprsPink))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (station.name == station.callsign) {
                            stringResource(R.string.map_aprs_station)
                        } else {
                            stringResource(R.string.map_aprs_object, station.callsign)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                station.receivedAt?.let { heard ->
                    Text(
                        text = stringResource(
                            R.string.map_aprs_heard,
                            DateFormats.malaysiaDateTime(heard.toEpochMilli(), use24Hour),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (position != null) {
                    Text(
                        text = stringResource(
                            if (position.source == LocationMode.DEVICE) R.string.repeater_distance_km
                            else R.string.repeater_distance_km_approx,
                            RepeaterFormat.distance(Geo.distanceKm(position.point, station.point)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (station.comment.isNotBlank()) {
                    Text(
                        text = station.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The map's info window: the place, how far it is, and each repeater on the site. */
@Composable
private fun SiteCard(
    site: MapSite,
    position: UserPosition?,
    onOpenRepeater: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val first = site.repeaters.first()
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = listOf(first.location, first.state).filter { it.isNotBlank() }.joinToString(", "),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (position != null) {
                        Text(
                            text = stringResource(
                                if (position.source == LocationMode.DEVICE) R.string.repeater_distance_km
                                else R.string.repeater_distance_km_approx,
                                RepeaterFormat.distance(Geo.distanceKm(position.point, site.point)),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
            Column(
                modifier = Modifier
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(end = 12.dp),
            ) {
                site.repeaters.forEachIndexed { index, repeater ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SiteRepeaterRow(repeater) { onOpenRepeater(repeater.id) }
                }
            }
        }
    }
}

@Composable
private fun SiteRepeaterRow(repeater: Repeater, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = repeater.callsign,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = listOfNotNull(
                    repeater.offsetMhz?.let { stringResource(R.string.repeater_offset, RepeaterFormat.offset(it)) },
                    if (repeater.tone.isBlank()) stringResource(R.string.repeater_no_tone)
                    else stringResource(R.string.repeater_tone, repeater.tone),
                    repeater.mode.takeIf { it.isNotBlank() },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = repeater.downlinkMhz?.let { RepeaterFormat.frequency(it) } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = repeater.club,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
