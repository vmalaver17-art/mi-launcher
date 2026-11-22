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
    val setDefault = rememberSetDefaultLauncher()
    val byKey = remember(allApps) { allApps.associateBy { it.key } }
    val hidden = s.hidden.value.mapNotNull { byKey[it] }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0B))
            .systemBarsPadding()
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            Text("personalizar", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            // ---------------- Apariencia ----------------
            item { Section("Colores") }
            item { Column { Label("Color de acento"); AccentPicker(s) } }
            item {
                SliderRow("Transparencia de mosaicos", "${(s.tileAlpha.value * 100).roundToInt()}%", s.tileAlpha.value, 0.3f..1f, 13) {
                    s.tileAlpha.value = it
                }
            }
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

            // ---------------- Mosaicos ----------------
            item { Section("Mosaicos") }
            item { SwitchRow("Mostrar más mosaicos (6 columnas)", s.moreTiles.value) { s.moreTiles.value = it } }
            item { SwitchRow("Íconos blancos (estilo Windows Phone)", s.monoIcons.value) { s.monoIcons.value = it } }
            item { SwitchRow("Nombres en los mosaicos", s.tileLabels.value) { s.tileLabels.value = it } }
            item { SwitchRow("Mosaico de reloj", s.isPinned(CLOCK_TILE)) { s.togglePin(CLOCK_TILE) } }
            item { SwitchRow("Reloj de 24 horas", s.clock24.value) { s.clock24.value = it } }
            item { SwitchRow("Mostrar fecha en el reloj", s.showDate.value) { s.showDate.value = it } }
            item {
                OutlinedButton(onClick = { s.initDefaults(allApps) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Restablecer mosaicos de inicio")
                }
            }

            // ---------------- Texto ----------------
            item { Section("Texto") }
            item { Column { Label("Fuente"); Chips(FONT_NAMES, s.font.value) { s.font.value = it } } }
            item {
                SliderRow(
                    "Tamaño de texto", "${(s.textScale.value * 100).roundToInt()}%",
                    s.textScale.value, 0.8f..1.5f, 13,
                ) { s.textScale.value = it }
            }

            // ---------------- Ocultas ----------------
            item { Section("Apps ocultas") }
            if (hidden.isEmpty()) item { Hint("Ninguna. Puedes ocultar apps desde la lista de apps.") }
            items(hidden, key = { "hid" + it.key }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(Color(s.colorOf(app.key))), contentAlignment = Alignment.Center) {
                        TileIcon(s, app, Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(s.label(app), color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { s.unhide(app.key) }) { Text("Mostrar") }
                }
            }

            // ---------------- Sistema ----------------
            item { Section("Sistema") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { setDefault() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Usar como launcher predeterminado")
                    }
                    OutlinedButton(onClick = { s.resetLook() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Restablecer apariencia")
                    }
                    Hint("Desliza a la izquierda = lista de apps · mantén presionado un mosaico = tamaño, color, mover o desanclar · toca una letra = saltar a otra letra · mantén presionado el fondo de inicio = este menú.")
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ACCENTS.forEach { c ->
            val selected = c == s.accent.value
            Box(
                Modifier
                    .size(44.dp)
                    .border(if (selected) 3.dp else 0.dp, if (selected) Color.White else Color.Transparent)
                    .padding(if (selected) 5.dp else 0.dp)
                    .background(Color(c))
                    .clickable { s.accent.value = c }
            )
        }
    }
}
