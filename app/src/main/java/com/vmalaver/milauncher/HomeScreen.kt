@file:OptIn(ExperimentalLayoutApi::class)

package com.vmalaver.milauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ES = Locale("es", "PE")

private data class Placed(val key: String, val x: Int, val y: Int, val w: Int, val h: Int) {
    fun overlaps(ox: Int, oy: Int, ow: Int, oh: Int) = x < ox + ow && ox < x + w && y < oy + oh && oy < y + h
}

private fun dims(size: String, cols: Int): Pair<Int, Int> {
    val (w, h) = when (size) { "s" -> 1 to 1; "w" -> 4 to 2; else -> 2 to 2 }
    return minOf(w, cols) to h
}

/**
 * Acomoda los mosaicos: primero los que tienen posición guardada (donde el usuario los dejó),
 * luego el resto en el primer hueco libre.
 */
private fun pack(
    keys: List<String>,
    sizeOf: (String) -> String,
    posOf: (String) -> Pair<Int, Int>?,
    cols: Int,
): Pair<List<Placed>, Int> {
    val occ = ArrayList<BooleanArray>()
    fun row(y: Int): BooleanArray { while (occ.size <= y) occ.add(BooleanArray(cols)); return occ[y] }
    fun free(x: Int, y: Int, w: Int, h: Int): Boolean {
        if (x < 0 || y < 0 || x + w > cols) return false
        for (yy in y until y + h) for (xx in x until x + w) if (row(yy)[xx]) return false
        return true
    }
    fun mark(x: Int, y: Int, w: Int, h: Int) {
        for (yy in y until y + h) for (xx in x until x + w) row(yy)[xx] = true
    }
    val out = ArrayList<Placed>()
    val pending = ArrayList<String>()
    for (k in keys) {
        val (w, h) = dims(sizeOf(k), cols)
        val pos = posOf(k)
        if (pos != null) {
            val x = pos.first.coerceIn(0, cols - w)
            val y = pos.second.coerceAtLeast(0)
            if (free(x, y, w, h)) { mark(x, y, w, h); out += Placed(k, x, y, w, h); continue }
        }
        pending += k
    }
    for (k in pending) {
        val (w, h) = dims(sizeOf(k), cols)
        var y = 0
        var done = false
        while (!done) {
            for (x in 0..cols - w) {
                if (free(x, y, w, h)) { mark(x, y, w, h); out += Placed(k, x, y, w, h); done = true; break }
            }
            if (!done) y++
        }
    }
    val rows = out.maxOfOrNull { it.y + it.h } ?: 0
    return out to rows
}

@Composable
fun StartScreen(
    s: LauncherSettings,
    apps: List<AppInfo>,
    isDefault: Boolean,
    notifAccess: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val openSettings by rememberUpdatedState(onOpenSettings)
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val keys = s.tiles.value.filter { it == CLOCK_TILE || it in byKey }
    val cols = if (s.moreTiles.value) 6 else 4
    val (placed, rows) = remember(keys, s.sizes.value, s.positions.value, cols) {
        pack(keys, s::sizeOf, s::posOf, cols)
    }

    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var menuKey by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState(), enabled = dragKey == null)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { openSettings() }) }
            .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 24.dp),
    ) {
        if (!isDefault) {
            val setDefault = rememberSetDefaultLauncher()
            Banner(s, "Toca aquí para usar Mi Launcher como pantalla de inicio") { setDefault() }
        }
        if (s.badges.value && !notifAccess) {
            Banner(s, "Toca aquí para ver las notificaciones en los mosaicos (activa \"Mi Launcher\")") {
                Badges.requestAccess(context)
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
            val stepPx = with(density) { (unit + gap).toPx() }
            // Al arrastrar, dejar espacio extra abajo para poder soltar más lejos
            val shownRows = rows + if (dragKey != null) 4 else 0

            fun drop(p: Placed, total: Offset) {
                val nx = (p.x + (total.x / stepPx).roundToInt()).coerceIn(0, cols - p.w)
                val ny = (p.y + (total.y / stepPx).roundToInt()).coerceAtLeast(0)
                val newPos = HashMap<String, String>()
                // Los demás se quedan donde están; si quedan debajo del que se soltó, se reacomodan solos
                placed.filter { it.key != p.key && !it.overlaps(nx, ny, p.w, p.h) }
                    .forEach { newPos[it.key] = "${it.x},${it.y}" }
                newPos[p.key] = "$nx,$ny"
                s.positions.value = newPos
            }

            Box(Modifier.fillMaxWidth().height(if (shownRows == 0) 0.dp else unit * shownRows + gap * (shownRows - 1))) {
                // Guía de celdas mientras se arrastra
                if (dragKey != null) {
                    for (y in 0 until shownRows) for (x in 0 until cols) {
                        Box(
                            Modifier
                                .offset(x = (unit + gap) * x, y = (unit + gap) * y)
                                .size(unit)
                                .border(1.dp, Color.White.copy(alpha = 0.12f))
                        )
                    }
                }
                placed.forEach { p ->
                    val dragging = dragKey == p.key
                    Tile(
                        s = s,
                        key = p.key,
                        app = byKey[p.key],
                        w = p.w,
                        h = p.h,
                        menuOpen = menuKey == p.key,
                        onDismissMenu = { menuKey = null },
                        modifier = Modifier
                            .offset(x = (unit + gap) * p.x, y = (unit + gap) * p.y)
                            .size(width = unit * p.w + gap * (p.w - 1), height = unit * p.h + gap * (p.h - 1))
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                if (dragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.06f
                                    scaleY = 1.06f
                                    alpha = 0.9f
                                } else if (dragKey != null) {
                                    scaleX = 0.94f
                                    scaleY = 0.94f
                                }
                            }
                            .pointerInput(p, placed, stepPx, byKey) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    val lp = awaitLongPressOrCancellation(down.id)
                                    if (lp == null) {
                                        // Toque normal: abrir la app
                                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                        if (up != null && !up.pressed && !up.isConsumed) {
                                            up.consume()
                                            val app = byKey[p.key]
                                            if (app != null) onLaunch(app) else Apps.openClock(context)
                                        }
                                    } else {
                                        // Mantener presionado: arrastrar para mover, o soltar para ver opciones
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragKey = p.key
                                        dragOffset = Offset.Zero
                                        var total = Offset.Zero
                                        drag(lp.id) { ch ->
                                            total += ch.positionChange()
                                            ch.consume()
                                            dragOffset = total
                                        }
                                        if (total.getDistance() < stepPx / 3) menuKey = p.key
                                        else drop(p, total)
                                        dragKey = null
                                        dragOffset = Offset.Zero
                                    }
                                }
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
private fun Banner(s: LauncherSettings, text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Color(s.accent.value))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(text, style = bodyStyle(s, 15f).copy(color = Color.White))
    }
}

@Composable
private fun Tile(
    s: LauncherSettings,
    key: String,
    app: AppInfo?,
    w: Int,
    h: Int,
    menuOpen: Boolean,
    onDismissMenu: () -> Unit,
    modifier: Modifier,
) {
    var pickColor by remember { mutableStateOf(false) }
    val bg = Color(s.colorOf(key)).copy(alpha = s.tileAlpha.value)
    val count = if (s.badges.value && app != null) Badges.counts[app.component.packageName] ?: 0 else 0

    Box(modifier) {
        Box(Modifier.fillMaxSize().background(bg)) {
            if (key == CLOCK_TILE) {
                ClockTile(s, small = w == 1)
            } else if (app != null) {
                val iconFraction = if (w == 1) 0.62f else 0.42f
                TileIcon(s, app, Modifier.align(Alignment.Center).fillMaxHeight(iconFraction).aspectRatio(1f))
                if (s.tileLabels.value && w > 1) {
                    Text(
                        s.label(app),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 6.dp, end = if (count > 0) 40.dp else 8.dp),
                        style = bodyStyle(s, 13f).copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (count > 0) {
                    val txt = if (count > 99) "99+" else count.toString()
                    if (w == 1) {
                        Text(
                            txt,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
                            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        )
                    } else {
                        Text(
                            txt,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 2.dp),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = (22 * s.textScale.value).sp,
                                fontFamily = s.fontFamily,
                                fontWeight = FontWeight.Light,
                            ),
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
            SIZE_NAMES.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text((if (s.sizeOf(key) == code) "● " else "○ ") + name) },
                    onClick = { s.setSize(key, code); onDismissMenu() },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Color del mosaico") }, onClick = { onDismissMenu(); pickColor = true })
            DropdownMenuItem(text = { Text("Desanclar") }, onClick = { s.togglePin(key); onDismissMenu() })
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
