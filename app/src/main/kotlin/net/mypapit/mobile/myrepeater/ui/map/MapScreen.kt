package net.mypapit.mobile.myrepeater.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.ui.UserPosition
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import net.mypapit.mobile.myrepeater.ui.theme.MixedBandPurple
import net.mypapit.mobile.myrepeater.ui.theme.SignalOrange
import net.mypapit.mobile.myrepeater.ui.theme.SkyBlue

/** Map of every repeater with known coordinates. Tapping a pin opens an info card for its site. */
@Composable
fun MapScreen(repeaters: List<Repeater>, position: UserPosition?) {
    val sites = remember(repeaters) { MapSites.from(repeaters) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = remember(sites, selectedId) { sites.firstOrNull { it.id == selectedId } }
    var lastSelected by remember { mutableStateOf<MapSite?>(null) }
    LaunchedEffect(selected) { if (selected != null) lastSelected = selected }
    var basemapUnavailable by remember { mutableStateOf(false) }
    // Centre on wherever the user was when the map first opened; later moves do not yank the camera.
    val focus = remember { position?.point }
    var savedCamera by rememberSaveable { mutableStateOf<DoubleArray?>(null) }

    Box(Modifier.fillMaxSize()) {
        RepeaterMapView(
            sites = sites,
            userPosition = position?.takeIf { it.source == LocationMode.DEVICE }?.point,
            focus = focus,
            savedCamera = savedCamera,
            onCameraIdle = { savedCamera = it },
            onSiteTapped = { selectedId = it?.id },
            onBasemapUnavailable = { basemapUnavailable = it },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Legend()
            if (basemapUnavailable) {
                Chip { Text(stringResource(R.string.map_offline_notice), style = MaterialTheme.typography.labelMedium) }
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
                SiteCard(site, position, onClose = { selectedId = null })
            }
        }
    }
}

@Composable
private fun Legend() {
    Chip {
        LegendDot(SignalOrange, stringResource(R.string.map_legend_2m))
        Spacer(Modifier.width(12.dp))
        LegendDot(SkyBlue, stringResource(R.string.map_legend_70cm))
        Spacer(Modifier.width(12.dp))
        LegendDot(MixedBandPurple, stringResource(R.string.map_legend_mixed))
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

/** The map's info window: the place, how far it is, and each repeater on the site. */
@Composable
private fun SiteCard(site: MapSite, position: UserPosition?, onClose: () -> Unit) {
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
                    SiteRepeaterRow(repeater)
                }
            }
        }
    }
}

@Composable
private fun SiteRepeaterRow(repeater: Repeater) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
