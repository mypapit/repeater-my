package net.mypapit.mobile.myrepeater.ui.sync

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.sync.Dataset
import net.mypapit.mobile.myrepeater.data.sync.SyncFailure
import net.mypapit.mobile.myrepeater.data.sync.SyncState

private const val QUOTE_INTERVAL_MS = 2_800L

/** First-start screen: download progress, a rotating quote, and a retry when offline. */
@Composable
fun SyncScreen(state: SyncState, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sync_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            if (state is SyncState.Failed) {
                Text(
                    text = stringResource(
                        when (state.reason) {
                            SyncFailure.NETWORK -> R.string.sync_failed_network
                            SyncFailure.BAD_DATA -> R.string.sync_failed_data
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRetry) { Text(stringResource(R.string.sync_retry)) }
            } else {
                Progress(state)
                Spacer(Modifier.height(40.dp))
                RotatingQuote()
            }
        }
    }
}

@Composable
private fun Progress(state: SyncState) {
    val fraction = (state as? SyncState.Downloading)?.fraction
    if (fraction != null) {
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(
            when (state) {
                is SyncState.Downloading -> when (state.dataset) {
                    Dataset.REPEATERS -> R.string.sync_downloading_repeaters
                    Dataset.CALLSIGNS -> R.string.sync_downloading_callsigns
                }
                is SyncState.Processing -> when (state.dataset) {
                    Dataset.REPEATERS -> R.string.sync_processing_repeaters
                    Dataset.CALLSIGNS -> R.string.sync_processing_callsigns
                }
                else -> R.string.sync_starting
            },
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RotatingQuote() {
    val quotes = stringArrayResource(R.array.loading_quotes)
    var index by remember { mutableIntStateOf(quotes.indices.random()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(QUOTE_INTERVAL_MS)
            index = (index + 1) % quotes.size
        }
    }
    AnimatedContent(
        targetState = index,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
        },
        label = "quote",
    ) { current ->
        Text(
            text = quotes[current],
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
        )
    }
}
