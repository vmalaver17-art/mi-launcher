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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var settings: LauncherSettings
    private val apps = mutableStateOf<List<AppInfo>>(emptyList())
    private val homeSignal = mutableIntStateOf(0)
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        settings = LauncherSettings(this)
        setContent {
            LauncherApp(settings, apps.value, homeSignal.intValue)
        }
    }

    override fun onResume() {
        super.onResume()
        reload() // refresca la lista por si se instaló o desinstaló algo
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue++ // botón Inicio: volver a los mosaicos
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
fun LauncherApp(s: LauncherSettings, allApps: List<AppInfo>, homeSignal: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = Color(s.accent.value)
    val scheme = darkColorScheme(
        primary = accent,
        secondary = accent,
        tertiary = accent,
        onPrimary = Color.White,
        background = Color.Black,
        surface = Color(0xFF1F1F1F),
        surfaceContainer = Color(0xFF1F1F1F),
    )
    val pager = rememberPagerState { 2 }
    var showSettings by remember { mutableStateOf(false) }

    val hiddenKeys = s.hidden.value
    val renames = s.renames.value
    val visible = remember(allApps, hiddenKeys, renames) {
        allApps.filter { it.key !in hiddenKeys }.sortedBy { normalize(s.label(it)) }
    }

    // Primera vez: poner mosaicos por defecto
    LaunchedEffect(allApps) {
        if (!s.initialized.value && allApps.isNotEmpty()) s.initDefaults(allApps)
    }
    // Botón Inicio: volver a los mosaicos
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            showSettings = false
            pager.animateScrollToPage(0)
        }
    }

    val launch: (AppInfo) -> Unit = { app ->
        Apps.launch(context, app)
        scope.launch { pager.scrollToPage(0) }
    }

    MaterialTheme(colorScheme = scheme) {
        BackHandler {
            when {
                showSettings -> showSettings = false
                pager.currentPage != 0 -> scope.launch { pager.animateScrollToPage(0) }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = s.bgDim.value))
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    StartScreen(
                        s = s,
                        apps = visible,
                        onLaunch = launch,
                        onOpenList = { scope.launch { pager.animateScrollToPage(1) } },
                        onOpenSettings = { showSettings = true },
                    )
                } else {
                    AppListScreen(
                        s = s,
                        apps = visible,
                        active = pager.currentPage == 1,
                        onLaunch = launch,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
            AnimatedVisibility(visible = showSettings, enter = fadeIn(), exit = fadeOut()) {
                SettingsScreen(s = s, allApps = allApps, onBack = { showSettings = false })
            }
        }
    }
}
