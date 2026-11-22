package com.vmalaver.milauncher

import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { HOME, DRAWER, SETTINGS }

class MainActivity : ComponentActivity() {
    private lateinit var settings: LauncherSettings
    private val apps = mutableStateOf<List<AppInfo>>(emptyList())
    private val screen = mutableStateOf(Screen.HOME)
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        settings = LauncherSettings(this)
        setContent {
            LauncherApp(settings, apps.value, screen.value) { screen.value = it }
        }
    }

    override fun onResume() {
        super.onResume()
        reload() // refresca la lista por si se instaló o desinstaló algo
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen.value = Screen.HOME // botón Inicio: volver a la pantalla principal
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val list = withContext(Dispatchers.Default) { runCatching { Apps.load(this@MainActivity) }.getOrNull() }
            if (list != null) apps.value = list
        }
    }
}

@Composable
fun LauncherApp(
    s: LauncherSettings,
    allApps: List<AppInfo>,
    screen: Screen,
    onScreen: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val accent = Color(s.accent.value)
    val scheme = darkColorScheme(
        primary = accent,
        secondary = accent,
        tertiary = accent,
        onPrimary = Color.Black,
        background = Color.Black,
        surface = Color(0xFF151517),
        surfaceContainer = Color(0xFF1C1C1F),
    )
    val hiddenKeys = s.hidden.value
    val renames = s.renames.value
    val visible = remember(allApps, hiddenKeys, renames) {
        allApps.filter { it.key !in hiddenKeys }.sortedBy { normalize(s.label(it)) }
    }
    val launch: (AppInfo) -> Unit = { app ->
        Apps.launch(context, app)
        onScreen(Screen.HOME)
    }

    MaterialTheme(colorScheme = scheme) {
        BackHandler { onScreen(Screen.HOME) } // en inicio, "atrás" no hace nada
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = s.bgDim.value))
        ) {
            HomeScreen(
                s = s,
                apps = visible,
                onLaunch = launch,
                onOpenDrawer = { onScreen(Screen.DRAWER) },
                onOpenSettings = { onScreen(Screen.SETTINGS) },
            )
            AnimatedVisibility(
                visible = screen == Screen.DRAWER,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) {
                DrawerScreen(
                    s = s,
                    apps = visible,
                    onLaunch = launch,
                    onClose = { onScreen(Screen.HOME) },
                    onOpenSettings = { onScreen(Screen.SETTINGS) },
                )
            }
            AnimatedVisibility(
                visible = screen == Screen.SETTINGS,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SettingsScreen(s = s, allApps = allApps, onBack = { onScreen(Screen.HOME) })
            }
        }
    }
}
