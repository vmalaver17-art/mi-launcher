package com.vmalaver.milauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ES = Locale("es", "PE")

@Composable
fun HomeScreen(
    s: LauncherSettings,
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val openSettings by rememberUpdatedState(onOpenSettings)
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favs = s.favorites.value.mapNotNull { byKey[it] }
    var drag by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxSize()
            // Mantener presionado el fondo = ajustes
            .pointerInput(Unit) { detectTapGestures(onLongPress = { openSettings() }) }
            // Deslizar arriba = cajón de apps · deslizar abajo = notificaciones
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag < -120f) openDrawer()
                        else if (drag > 120f) Apps.expandNotifications(context)
                        drag = 0f
                    },
                ) { change, amount -> change.consume(); drag += amount }
            }
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = s.hAlign,
    ) {
        Spacer(Modifier.size(24.dp))
        Clock(s)

        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = when (s.align.value) {
                1 -> Alignment.Center; 2 -> Alignment.CenterEnd; else -> Alignment.CenterStart
            },
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = s.hAlign,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (favs.isEmpty()) {
                    Text(
                        "Desliza hacia arriba para ver tus apps.\nMantén presionada una app para fijarla aquí.",
                        style = bodyStyle(s, 14f).copy(color = Color.White.copy(alpha = 0.45f), textAlign = s.textAlign),
                    )
                }
                favs.forEach { app -> FavoriteRow(s, app, onClick = { onLaunch(app) }) }
            }
        }

        Text(
            "︿",
            modifier = Modifier
                .clickable(remember { MutableInteractionSource() }, null) { openDrawer() }
                .padding(8.dp),
            style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp),
        )
    }
}

@Composable
private fun Clock(s: LauncherSettings) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - value % 1000L)
        }
    }
    val time = SimpleDateFormat(if (s.clock24.value) "HH:mm" else "h:mm", ES).format(Date(now))
    val date = SimpleDateFormat("EEEE, d 'de' MMMM", ES).format(Date(now))
        .replaceFirstChar { it.uppercase() }

    Column(
        Modifier.clickable(remember { MutableInteractionSource() }, null) { Apps.openClock(context) },
        horizontalAlignment = s.hAlign,
    ) {
        Text(
            time,
            style = TextStyle(
                color = Color.White,
                fontSize = s.clockSize.value.sp,
                fontFamily = s.fontFamily,
                fontWeight = s.clockWeight,
                letterSpacing = (-1).sp,
            ),
        )
        if (s.showDate.value) {
            Text(date, style = bodyStyle(s, 16f).copy(color = Color(s.accent.value)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(s: LauncherSettings, app: AppInfo, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (s.iconsHome.value) {
                AppIcon(app.icon, s.iconSize.value * 0.8f)
                Spacer(Modifier.width(14.dp))
            }
            Text(
                s.label(app),
                style = bodyStyle(s, 24f).copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Subir") }, onClick = { s.moveFavorite(app.key, -1); menu = false })
            DropdownMenuItem(text = { Text("Bajar") }, onClick = { s.moveFavorite(app.key, 1); menu = false })
            DropdownMenuItem(text = { Text("Quitar de inicio") }, onClick = { s.toggleFavorite(app.key); menu = false })
        }
    }
}

@Composable
fun AppIcon(icon: ImageBitmap, sizeDp: Float) {
    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
}

fun bodyStyle(s: LauncherSettings, baseSp: Float) = TextStyle(
    fontSize = (baseSp * s.textScale.value).sp,
    fontFamily = s.fontFamily,
    fontWeight = s.bodyWeight,
)
