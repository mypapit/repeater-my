package net.mypapit.mobile.myrepeater.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.BuildConfig
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.ui.theme.AppLogo

private const val GITHUB_URL = "https://github.com/mypapit/repeater-my"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** Who made the app, the terms it is released under, and where its source lives. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    // A phone with no browser throws here; there is nothing useful to do about it.
    val open = { url: String -> runCatching { uriHandler.openUri(url) }.let { } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
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
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppLogo(size = 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            AboutCard(stringResource(R.string.about_copyright_title)) {
                Text(stringResource(R.string.about_copyright), style = MaterialTheme.typography.bodyLarge)
            }

            AboutCard(stringResource(R.string.about_licence_title)) {
                Paragraph(stringResource(R.string.about_licence_body))
                Paragraph(stringResource(R.string.about_licence_warranty))
                TextButton(onClick = { open(LICENCE_URL) }) {
                    Text(stringResource(R.string.about_licence_link))
                }
            }

            AboutCard(stringResource(R.string.about_source_title)) {
                Paragraph(stringResource(R.string.about_source_body))
                TextButton(onClick = { open(GITHUB_URL) }) { Text(GITHUB_URL.removePrefix("https://")) }
            }

            AboutCard(stringResource(R.string.about_disclaimer_title)) {
                Paragraph(stringResource(R.string.about_disclaimer_position))
                Paragraph(stringResource(R.string.about_disclaimer_data))
            }

            AboutCard(stringResource(R.string.about_credits_title)) {
                Paragraph(stringResource(R.string.about_credits_body))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AboutCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
