package net.mypapit.mobile.myrepeater.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.ui.theme.OffAirRed
import net.mypapit.mobile.myrepeater.ui.theme.OnAirGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterListScreen(repeaters: List<Repeater>, refreshing: Boolean) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.list_title))
                            Text(
                                text = pluralStringResource(R.plurals.list_count, repeaters.size, repeaters.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        if (repeaters.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
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
                items(repeaters, key = { it.id }) { repeater ->
                    RepeaterCard(repeater, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun RepeaterCard(repeater: Repeater, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PrefixAvatar(repeater.callsign)
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
            }
        }
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
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}
