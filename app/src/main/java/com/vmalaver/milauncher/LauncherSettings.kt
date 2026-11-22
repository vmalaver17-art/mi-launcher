package com.vmalaver.milauncher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject

// ---------- Preferencias observables (se guardan solas al cambiar) ----------

class PrefInt(private val p: SharedPreferences, private val key: String, private val def: Int) {
    private val state = mutableIntStateOf(p.getInt(key, def))
    var value: Int
        get() = state.intValue
        set(v) { state.intValue = v; p.edit().putInt(key, v).apply() }
    fun reset() { value = def }
}

class PrefFloat(private val p: SharedPreferences, private val key: String, private val def: Float) {
    private val state = mutableFloatStateOf(p.getFloat(key, def))
    var value: Float
        get() = state.floatValue
        set(v) { state.floatValue = v; p.edit().putFloat(key, v).apply() }
    fun reset() { value = def }
}

class PrefBool(private val p: SharedPreferences, private val key: String, private val def: Boolean) {
    private val state = mutableStateOf(p.getBoolean(key, def))
    var value: Boolean
        get() = state.value
        set(v) { state.value = v; p.edit().putBoolean(key, v).apply() }
    fun reset() { value = def }
}

class PrefList(private val p: SharedPreferences, private val key: String) {
    private val state = mutableStateOf(read())
    private fun read(): List<String> = runCatching {
        val arr = JSONArray(p.getString(key, "[]"))
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())
    var value: List<String>
        get() = state.value
        set(v) { state.value = v; p.edit().putString(key, JSONArray(v).toString()).apply() }
}

class PrefMap(private val p: SharedPreferences, private val key: String) {
    private val state = mutableStateOf(read())
    private fun read(): Map<String, String> = runCatching {
        val o = JSONObject(p.getString(key, "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())
    var value: Map<String, String>
        get() = state.value
        set(v) { state.value = v; p.edit().putString(key, JSONObject(v).toString()).apply() }
}

// ---------- Estilo Windows Phone ----------

/** Los 20 colores de acento de Windows Phone. */
val ACCENTS = listOf(
    0xFFA4C400, 0xFF60A917, 0xFF008A00, 0xFF00ABA9, 0xFF1BA1E2,
    0xFF0050EF, 0xFF6A00FF, 0xFFAA00FF, 0xFFF472D0, 0xFFD80073,
    0xFFA20025, 0xFFE51400, 0xFFFA6800, 0xFFF0A30A, 0xFFE3C800,
    0xFF825A2C, 0xFF6D8764, 0xFF647687, 0xFF76608A, 0xFF87794E,
).map { it.toInt() }

val FONT_NAMES = listOf("Sistema", "Fina", "Serif", "Mono")

const val CLOCK_TILE = "__reloj__"

/** Tamaños de mosaico: s = pequeño (1x1), m = mediano (2x2), w = ancho (4x2). */
val SIZE_NAMES = listOf("s" to "Pequeño", "m" to "Mediano", "w" to "Ancho")

class LauncherSettings(context: Context) {
    private val p = context.getSharedPreferences("mi_launcher_wp", Context.MODE_PRIVATE)

    // Apariencia
    val accent = PrefInt(p, "accent", ACCENTS[5])      // azul cobalto
    val bgDim = PrefFloat(p, "bg_dim", 1f)              // 1 = negro puro (tema oscuro WP)
    val tileAlpha = PrefFloat(p, "tile_alpha", 1f)
    val monoIcons = PrefBool(p, "mono_icons", true)     // íconos blancos como en WP
    val moreTiles = PrefBool(p, "more_tiles", false)    // 6 columnas en vez de 4
    val tileLabels = PrefBool(p, "tile_labels", true)
    val badges = PrefBool(p, "badges", true)            // número de notificaciones en mosaicos
    val font = PrefInt(p, "font", 1)
    val textScale = PrefFloat(p, "text_scale", 1f)

    // Reloj
    val clock24 = PrefBool(p, "clock24", true)
    val showDate = PrefBool(p, "show_date", true)

    // Datos
    val tiles = PrefList(p, "tiles")
    val sizes = PrefMap(p, "sizes")
    val colors = PrefMap(p, "colors")
    val positions = PrefMap(p, "positions")          // posición libre de cada mosaico: "x,y"
    val hidden = PrefList(p, "hidden")
    val renames = PrefMap(p, "renames")
    val initialized = PrefBool(p, "initialized", false)

    fun label(app: AppInfo): String = renames.value[app.key]?.takeIf { it.isNotBlank() } ?: app.label

    fun isPinned(key: String) = key in tiles.value
    fun togglePin(key: String) {
        if (isPinned(key)) {
            tiles.value = tiles.value - key
            positions.value = positions.value - key
        } else {
            tiles.value = tiles.value + key
        }
    }
    fun posOf(key: String): Pair<Int, Int>? = positions.value[key]?.split(',')?.let { p ->
        val x = p.getOrNull(0)?.toIntOrNull(); val y = p.getOrNull(1)?.toIntOrNull()
        if (x != null && y != null) x to y else null
    }
    fun sizeOf(key: String): String = sizes.value[key] ?: if (key == CLOCK_TILE) "w" else "m"
    fun setSize(key: String, size: String) { sizes.value = sizes.value + (key to size) }
    fun colorOf(key: String): Int = colors.value[key]?.toIntOrNull() ?: accent.value
    fun setColor(key: String, color: Int?) {
        colors.value = if (color == null) colors.value - key else colors.value + (key to color.toString())
    }
    fun move(key: String, delta: Int) {
        val l = tiles.value.toMutableList()
        val i = l.indexOf(key); val j = i + delta
        if (i < 0 || j !in l.indices) return
        l[i] = l[j].also { l[j] = l[i] }
        tiles.value = l
    }
    fun hide(key: String) {
        hidden.value = (hidden.value + key).distinct()
        tiles.value = tiles.value - key
        positions.value = positions.value - key
    }
    fun unhide(key: String) { hidden.value = hidden.value - key }
    fun rename(key: String, name: String) {
        renames.value = if (name.isBlank()) renames.value - key else renames.value + (key to name.trim())
    }

    /** Mosaicos iniciales: reloj ancho + apps comunes si están instaladas. */
    fun initDefaults(apps: List<AppInfo>) {
        val wanted = listOf(
            listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer") to "m",
            listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms") to "m",
            listOf("com.whatsapp") to "m",
            listOf("com.android.chrome", "com.sec.android.app.sbrowser") to "s",
            listOf("com.google.android.gm") to "s",
            listOf("com.google.android.apps.maps") to "s",
            listOf("com.google.android.youtube") to "s",
            listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery") to "m",
            listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2") to "m",
            listOf("com.android.settings") to "s",
        )
        val t = mutableListOf(CLOCK_TILE)
        val sz = mutableMapOf(CLOCK_TILE to "w")
        for ((pkgs, size) in wanted) {
            val app = pkgs.firstNotNullOfOrNull { pkg -> apps.firstOrNull { it.component.packageName == pkg } } ?: continue
            if (app.key !in t) { t += app.key; sz[app.key] = size }
        }
        tiles.value = t
        sizes.value = sz
        colors.value = emptyMap()
        positions.value = emptyMap()
        initialized.value = true
    }

    fun resetLook() {
        accent.reset(); font.reset()
        bgDim.reset(); tileAlpha.reset(); textScale.reset()
        monoIcons.reset(); moreTiles.reset(); tileLabels.reset(); badges.reset(); clock24.reset(); showDate.reset()
    }

    val fontFamily: FontFamily
        get() = when (font.value) { 2 -> FontFamily.Serif; 3 -> FontFamily.Monospace; else -> FontFamily.SansSerif }
    val bodyWeight: FontWeight
        get() = if (font.value == 1) FontWeight.Light else FontWeight.Normal
}
