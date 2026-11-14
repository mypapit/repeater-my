package net.mypapit.mobile.myrepeater.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.ui.UserPosition
import net.mypapit.mobile.myrepeater.ui.theme.OffAirRed
import net.mypapit.mobile.myrepeater.ui.theme.OnAirGreen

/**
 * @param waitingForLocation device location is allowed but no fix has arrived yet
 * @param heading true heading of the top of the screen; read lazily so that a
 *   turning phone only redraws the arrows, not the whole list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterListScreen(
    items: List<RepeaterListItem>,
    totalCount: Int,
    filter: RepeaterFilter,
    bands: List<String>,
    states: List<String>,
    position: UserPosition?,
    waitingForLocation: Boolean,
    heading: () -> Float?,
    refreshing: Boolean,
    onFilterChange: (RepeaterFilter) -> Unit,
    onOpenRepeater: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val onDevice = position?.source == LocationMode.DEVICE
    // Derived so that a turning phone does not recompose the whole list.
    val hasHeading by remember(heading) { derivedStateOf { heading() != null } }
    val compassShown = onDevice && hasHeading

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.list_title))
                            Text(
                                text = if (filter.isActive) {
                                    stringResource(R.string.list_count_filtered, items.size, totalCount)
                                } else {
                                    pluralStringResource(R.plurals.list_count, items.size, items.size)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                FilterBar(filter, bands, states, onFilterChange)
                if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header", contentType = "header") {
                LocationHeader(position, waitingForLocation, compassShown, onOpenSettings)
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(if (filter.isActive) R.string.list_no_match else R.string.list_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (filter.isActive) {
                            TextButton(onClick = { onFilterChange(RepeaterFilter()) }) {
                                Text(stringResource(R.string.filter_clear))
                            }
                        }
                    }
                }
            }
            items(items, key = { it.repeater.id }, contentType = { "repeater" }) { item ->
                RepeaterCard(item, onDevice, compassShown, heading, { onOpenRepeater(item.repeater.id) }, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun LocationHeader(
    position: UserPosition?,
    waiting: Boolean,
    compassShown: Boolean,
    onOpenSettings: () -> Unit,
) {
    when {
        position?.source == LocationMode.DEVICE -> Column {
            SectionLabel(stringResource(R.string.list_nearest_to_you))
            if (compassShown) {
                Text(
                    text = stringResource(R.string.list_compass_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        position != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.list_nearest_to_place, position.placeName.orEmpty()))
            }
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.list_change_place)) }
        }

        waiting -> SectionLabel(stringResource(R.string.list_locating))

        else -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.list_location_off_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.list_location_off_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.list_location_off_action))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RepeaterCard(
    item: RepeaterListItem,
    onDevice: Boolean,
    compassShown: Boolean,
    heading: () -> Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeater = item.repeater
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val distance = item.distanceKm
            val bearing = item.bearingDeg
            when {
                // The compass only makes sense from the phone's own position.
                !onDevice || distance == null || bearing == null -> PrefixAvatar(repeater.callsign)
                distance <= Geo.COMPASS_MIN_DISTANCE_KM -> NearbyBadge()
                !compassShown -> PrefixAvatar(repeater.callsign)
                else -> CompassArrow(bearing.toFloat(), heading)
            }
            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = repeater.callsign,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusDot(repeater.status)
                }
                Text(
                    text = listOf(repeater.location, repeater.state).filter { it.isNotBlank() }.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        repeater.offsetMhz?.let { stringResource(R.string.repeater_offset, RepeaterFormat.offset(it)) },
                        if (repeater.tone.isBlank()) stringResource(R.string.repeater_no_tone)
                        else stringResource(R.string.repeater_tone, repeater.tone),
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = repeater.downlinkMhz?.let { RepeaterFormat.frequency(it) } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = repeater.club,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (distance != null) {
                    Text(
                        text = stringResource(
                            if (onDevice) R.string.repeater_distance_km else R.string.repeater_distance_km_approx,
                            RepeaterFormat.distance(distance),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** An arrow that keeps pointing at the repeater as the phone turns. */
@Composable
private fun CompassArrow(bearing: Float, heading: () -> Float?) {
    val description = stringResource(R.string.repeater_direction_description)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_navigation),
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { rotationZ = bearing - (heading() ?: 0f) },
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Shown instead of the arrow inside the 10 km radius. */
@Composable
private fun NearbyBadge() {
    val description = stringResource(R.string.repeater_nearby_description)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.repeater_nearby),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Stands in for the compass while location is off: the callsign prefix on a seeded colour. */
@Composable
private fun PrefixAvatar(callsign: String) {
    val hue = RepeaterFormat.avatarHue(callsign)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.hsl(hue, saturation = 0.45f, lightness = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = RepeaterFormat.prefix(callsign),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.hsl(hue, saturation = 0.60f, lightness = 0.88f),
        )
    }
}

@Composable
private fun StatusDot(status: RepeaterStatus) {
    val color = when (status) {
        RepeaterStatus.ON_AIR -> OnAirGreen
        RepeaterStatus.OFF_AIR -> OffAirRed
        RepeaterStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    val description = stringResource(
        when (status) {
            RepeaterStatus.ON_AIR -> R.string.status_on_air
            RepeaterStatus.OFF_AIR -> R.string.status_off_air
            RepeaterStatus.UNKNOWN -> R.string.status_unknown
        },
    )
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = description },
    )
}
