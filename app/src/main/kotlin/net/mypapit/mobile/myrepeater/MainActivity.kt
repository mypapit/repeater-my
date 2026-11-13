package net.mypapit.mobile.myrepeater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.MainViewModel
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListScreen
import net.mypapit.mobile.myrepeater.ui.sync.SyncScreen
import net.mypapit.mobile.myrepeater.ui.theme.RepeaterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent),
        )
        super.onCreate(savedInstanceState)

        val container = (application as RepeaterApp).container
        setContent {
            RepeaterTheme {
                val viewModel: MainViewModel = viewModel { MainViewModel(container) }
                val dataReady by viewModel.dataReady.collectAsStateWithLifecycle()
                val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                val repeaters by viewModel.repeaters.collectAsStateWithLifecycle()

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Crossfade(targetState = dataReady, label = "root") { ready ->
                        when (ready) {
                            null -> Box(Modifier.fillMaxSize())
                            false -> SyncScreen(state = syncState, onRetry = viewModel::sync)
                            true -> RepeaterListScreen(
                                repeaters = repeaters,
                                refreshing = syncState is SyncState.Downloading || syncState is SyncState.Processing,
                            )
                        }
                    }
                }
            }
        }
    }
}
