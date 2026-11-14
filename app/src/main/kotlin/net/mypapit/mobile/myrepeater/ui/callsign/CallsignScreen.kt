package net.mypapit.mobile.myrepeater.ui.callsign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.CallsignRecord
import net.mypapit.mobile.myrepeater.ui.list.SearchField
import java.time.LocalDate

/** Look up a Malaysian amateur callsign or its holder. Searches run on the device only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsignScreen(
    query: String,
    state: CallsignSearchState,
    onQueryChange: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.callsign_title)) })
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.callsign_search_hint),
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }
        },
    ) { padding ->
        val today = remember { LocalDate.now() }
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
            when (state) {
                CallsignSearchState.TooShort -> item { Hint(stringResource(R.string.callsign_hint)) }
                is CallsignSearchState.Results -> {
                    if (state.records.isEmpty()) {
                        item { Hint(stringResource(R.string.callsign_none)) }
                    }
                    items(state.records, key = { it.id }) { record ->
                        CallsignCard(record, today, Modifier.animateItem())
                    }
                    if (state.truncated) {
                        item { Hint(stringResource(R.string.callsign_more, CallsignSearch.MAX_RESULTS)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CallsignCard(record: CallsignRecord, today: LocalDate, modifier: Modifier = Modifier) {
    val expired = remember(record.expiry, today) { CallsignSearch.isExpired(record.expiry, today) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.callsign,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(record.holder, style = MaterialTheme.typography.bodyLarge)
                if (record.expiry.isNotBlank()) {
                    Text(
                        text = stringResource(
                            if (expired == true) R.string.callsign_expired else R.string.callsign_expires,
                            record.expiry,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expired == true) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
