@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.vmalaver.milauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ES = Locale("es", "PE")

private data class Placed(val key: String, val x: Int, val y: Int, val w: Int, val h: Int)

/** Acomoda los mosaicos en la cuadrícula (primer hueco libre, como en Windows Phone). */
private fun pack(keys: List<String>, sizeOf: (String) -> String, cols: Int): Pair<List<Placed>, Int> {
    val occ = ArrayList<BooleanArray>()
    fun row(y: Int): BooleanArray { while (occ.size <= y) occ.add(BooleanArray(cols)); return occ[y] }
    fun free(x: Int, y: Int, w: Int, h: Int): Boolean {
        if (x + w > cols) return false
        for (yy in y until y + h) for (xx in x until x + w) if (row(yy)[xx]) return false
        return true
    }
    val out = ArrayList<Placed>()
    for (k in keys) {
        val (w0, h) = when (sizeOf(k)) { "s" -> 1 to 1; "w" -> 4 to 2; else -> 2 to 2 }
        val w = minOf(w0, cols)
        var y = 0
        var done = false
        while (!done) {
            for (x in 0..cols - w) {
                if (free(x, y, w, h)) {
                    for (yy in y until y + h) for (xx in x until x + w) row(yy)[xx] = true
                    out += Placed(k, x, y, w, h)
                    done = true
                    break
                }
            }
            if (!done) y++
        }
    }
    return out to occ.size
}

@Composable
fun StartScreen(
    s: LauncherSettings,
    apps: List<AppInfo>,
    isDefault: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val openSettings by rememberUpdatedState(onOpenSettings)
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val keys = s.tiles.value.filter { it == CLOCK_TILE || it in byKey }
    val cols = if (s.moreTiles.value) 6 else 4
    val (placed, rows) = remember(keys, s.sizes.value, cols) { pack(keys, s::sizeOf, cols) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) { detectTapGestures(onLongPress = { openSettings() }) }
            .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 24.dp),
    ) {
        if (!isDefault) {
            val setDefault = rememberSetDefaultLauncher()
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(s.accent.value))
                    .clickable { setDefault() }
                    .padding(14.dp),
            ) {
                Text(
                    "Toca aquí para usar Mi Launcher como pantalla de inicio",
                    style = bodyStyle(s, 15f).copy(color = Color.White),
                )
            }
        }
        if (keys.isEmpty()) {
            Text(
                "Desliza a la izquierda y mantén presionada una app para anclarla a inicio.",
                style = bodyStyle(s, 15f).copy(color = Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.padding(8.dp),
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 6.dp
            val unit = (maxWidth - gap * (cols - 1)) / cols
            Box(Modifier.fillMaxWidth().height(if (rows == 0) 0.dp else unit * rows + gap * (rows - 1))) {
                placed.forEach { p ->
                    Tile(
                        s = s,
                        key = p.key,
                        app = byKey[p.key],
                        w = p.w,
                        h = p.h,
                        modifier = Modifier
                            .offset(x = (unit + gap) * p.x, y = (unit + gap) * p.y)
                            .size(width = unit * p.w + gap * (p.w - 1), height = unit * p.h + gap * (p.h - 1)),
                        onClick = {
                            val app = byKey[p.key]
                            if (app != null) onLaunch(app) else Apps.openClock(context)
                        },
                    )
                }
            }
        }
        // Flecha para ir a la lista de apps (como en Windows Phone)
        Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier
                    .size(44.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onOpenList() },
                contentAlignment = Alignment.Center,
            ) {
                Text("→", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun Tile(
    s: LauncherSettings,
    key: String,
    app: AppInfo?,
    w: Int,
    h: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var pickColor by remember { mutableStateOf(false) }
    val bg = Color(s.colorOf(key)).copy(alpha = s.tileAlpha.value)

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        ) {
            if (key == CLOCK_TILE) {
                ClockTile(s, small = w == 1)
            } else if (app != null) {
                val iconFraction = if (w == 1) 0.62f else 0.42f
                TileIcon(s, app, Modifier.align(Alignment.Center).fillMaxHeight(iconFraction).aspectRatio(1f))
                if (s.tileLabels.value && w > 1) {
                    Text(
                        s.label(app),
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp, end = 8.dp),
                        style = bodyStyle(s, 13f).copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            SIZE_NAMES.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text((if (s.sizeOf(key) == code) "● " else "○ ") + name) },
                    onClick = { s.setSize(key, code); menu = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Color del mosaico") }, onClick = { menu = false; pickColor = true })
            DropdownMenuItem(text = { Text("Mover antes") }, onClick = { s.move(key, -1); menu = false })
            DropdownMenuItem(text = { Text("Mover después") }, onClick = { s.move(key, 1); menu = false })
            DropdownMenuItem(text = { Text("Desanclar") }, onClick = { s.togglePin(key); menu = false })
        }
    }

    if (pickColor) {
        AlertDialog(
            onDismissRequest = { pickColor = false },
            title = { Text("Color del mosaico") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ACCENTS.forEach { c ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(c))
                                .clickable { s.setColor(key, c); pickColor = false }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { s.setColor(key, null); pickColor = false }) { Text("Usar color de acento") }
            },
        )
    }
}

/** Ícono de app: silueta blanca (estilo WP) o ícono a color. */
@Composable
fun TileIcon(s: LauncherSettings, app: AppInfo, modifier: Modifier) {
    val glyph = app.glyph
    if (s.monoIcons.value && glyph != null) {
        Image(
            bitmap = glyph,
            contentDescription = null,
            modifier = modifier,
            colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn),
        )
    } else {
        Image(bitmap = app.icon, contentDescription = null, modifier = modifier)
    }
}

@Composable
private fun ClockTile(s: LauncherSettings, small: Boolean) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - value % 1000L)
        }
    }
    val time = SimpleDateFormat(if (s.clock24.value) "HH:mm" else "h:mm", ES).format(Date(now))
    if (small) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(time, style = bodyStyle(s, 16f).copy(color = Color.White))
        }
        return
    }
    val day = SimpleDateFormat("EEEE", ES).format(Date(now))
    val date = SimpleDateFormat("d 'de' MMMM", ES).format(Date(now))
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
        Text(
            time,
            style = TextStyle(
                color = Color.White,
                fontSize = (44 * s.textScale.value).sp,
                fontFamily = s.fontFamily,
                fontWeight = FontWeight.Light,
            ),
        )
        if (s.showDate.value) {
            Spacer(Modifier.height(2.dp))
            Text(day, style = bodyStyle(s, 15f).copy(color = Color.White))
            Text(date, style = bodyStyle(s, 15f).copy(color = Color.White.copy(alpha = 0.85f)))
        }
    }
}

fun bodyStyle(s: LauncherSettings, baseSp: Float) = TextStyle(
    fontSize = (baseSp * s.textScale.value).sp,
    fontFamily = s.fontFamily,
    fontWeight = s.bodyWeight,
)
