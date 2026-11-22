@file:OptIn(ExperimentalMaterial3Api::class)

package com.vmalaver.milauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(s: LauncherSettings, allApps: List<AppInfo>, onBack: () -> Unit) {
    val context = LocalContext.current
    val byKey = remember(allApps) { allApps.associateBy { it.key } }
    val favs = s.favorites.value.mapNotNull { byKey[it] }
    val hidden = s.hidden.value.mapNotNull { byKey[it] }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0B))
            .systemBarsPadding()
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            Text("Personalizar", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            // ---------------- Apariencia ----------------
            item { Section("Apariencia") }
            item { Column { Label("Color de acento"); AccentPicker(s) } }
            item {
                SliderRow(
                    "Oscurecer fondo de pantalla", "${(s.bgDim.value * 100).roundToInt()}%",
                    s.bgDim.value, 0f..1f, 19,
                ) { s.bgDim.value = it }
            }
            item {
                OutlinedButton(onClick = { Apps.pickWallpaper(context) }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Cambiar fondo de pantalla")
                }
            }
            item { Column { Label("Fuente"); Chips(FONT_NAMES, s.font.value) { s.font.value = it } } }
            item { Column { Label("Alineación"); Chips(ALIGN_NAMES, s.align.value) { s.align.value = it } } }
            item {
                SliderRow(
                    "Tamaño de texto", "${(s.textScale.value * 100).roundToInt()}%",
                    s.textScale.value, 0.8f..1.5f, 13,
                ) { s.textScale.value = it }
            }

            // ---------------- Reloj ----------------
            item { Section("Reloj") }
            item { SwitchRow("Formato 24 horas", s.clock24.value) { s.clock24.value = it } }
            item { SwitchRow("Mostrar fecha", s.showDate.value) { s.showDate.value = it } }
            item {
                SliderRow("Tamaño del reloj", "${s.clockSize.value.roundToInt()}", s.clockSize.value, 40f..120f, 15) {
                    s.clockSize.value = it
                }
            }

            // ---------------- Apps ----------------
            item { Section("Apps") }
            item { SwitchRow("Íconos en el cajón", s.iconsDrawer.value) { s.iconsDrawer.value = it } }
            item { SwitchRow("Íconos en inicio", s.iconsHome.value) { s.iconsHome.value = it } }
            item {
                SliderRow("Tamaño de íconos", "${s.iconSize.value.roundToInt()} dp", s.iconSize.value, 24f..64f, 9) {
                    s.iconSize.value = it
                }
            }
            item {
                val c = s.columns.value
                SliderRow(
                    "Columnas del cajón", if (c == 1) "Lista" else "$c",
                    c.toFloat(), 1f..5f, 3,
                ) { s.columns.value = it.roundToInt() }
            }
            item { SwitchRow("Abrir teclado al entrar al cajón", s.autoKeyboard.value) { s.autoKeyboard.value = it } }
            item {
                SwitchRow("Abrir la app si solo hay un resultado", s.autoLaunch.value) { s.autoLaunch.value = it }
            }

            // ---------------- Inicio ----------------
            item { Section("Apps en inicio") }
            if (favs.isEmpty()) item { Hint("En el cajón, mantén presionada una app → Fijar en inicio.") }
            items(favs, key = { "fav" + it.key }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.icon, 28f)
                    Spacer(Modifier.width(12.dp))
                    Text(s.label(app), color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { s.moveFavorite(app.key, -1) }) { Icon(Icons.Filled.KeyboardArrowUp, "Subir", tint = Color.White) }
                    IconButton(onClick = { s.moveFavorite(app.key, 1) }) { Icon(Icons.Filled.KeyboardArrowDown, "Bajar", tint = Color.White) }
                    IconButton(onClick = { s.toggleFavorite(app.key) }) { Icon(Icons.Filled.Close, "Quitar", tint = Color.White.copy(alpha = 0.6f)) }
                }
            }

            // ---------------- Ocultas ----------------
            item { Section("Apps ocultas") }
            if (hidden.isEmpty()) item { Hint("Ninguna. Puedes ocultar apps desde el cajón.") }
            items(hidden, key = { "hid" + it.key }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.icon, 28f)
                    Spacer(Modifier.width(12.dp))
                    Text(s.label(app), color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { s.unhide(app.key) }) { Text("Mostrar") }
                }
            }

            // ---------------- Sistema ----------------
            item { Section("Sistema") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Apps.openHomeSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Usar como launcher predeterminado")
                    }
                    OutlinedButton(onClick = { s.resetLook() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Restablecer apariencia")
                    }
                    Hint("Gestos: desliza arriba = apps · abajo = notificaciones · mantén presionado el fondo = este menú · toca el reloj = alarmas.")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Column {
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(Modifier.height(14.dp))
        Text(title.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, name ->
            FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(name) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(s: LauncherSettings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ACCENTS.forEach { c ->
            val selected = c == s.accent.value
            Box(
                Modifier
                    .size(36.dp)
                    .border(if (selected) 2.dp else 0.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(Color(c), CircleShape)
                    .clickable { s.accent.value = c }
            )
        }
    }
}
