package com.vmalaver.milauncher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// ---------- Ajustes del launcher ----------

val ACCENTS = listOf(
    0xFF8AB4F8, 0xFFFFFFFF, 0xFF81C995, 0xFF78D9EC, 0xFFC58AF9,
    0xFFFF8BCB, 0xFFF28B82, 0xFFFCAD70, 0xFFFDD663,
).map { it.toInt() }

val FONT_NAMES = listOf("Sistema", "Fina", "Serif", "Mono")
val ALIGN_NAMES = listOf("Izquierda", "Centro", "Derecha")

class LauncherSettings(context: Context) {
    private val p = context.getSharedPreferences("mi_launcher", Context.MODE_PRIVATE)

    // Apariencia
    val accent = PrefInt(p, "accent", ACCENTS[0])
    val bgDim = PrefFloat(p, "bg_dim", 0.35f)       // 0 = fondo de pantalla visible, 1 = negro puro
    val font = PrefInt(p, "font", 1)
    val align = PrefInt(p, "align", 0)
    val textScale = PrefFloat(p, "text_scale", 1f)

    // Reloj
    val clock24 = PrefBool(p, "clock24", true)
    val showDate = PrefBool(p, "show_date", true)
    val clockSize = PrefFloat(p, "clock_size", 76f)

    // Apps
    val iconsDrawer = PrefBool(p, "icons_drawer", true)
    val iconsHome = PrefBool(p, "icons_home", false)
    val iconSize = PrefFloat(p, "icon_size", 40f)
    val columns = PrefInt(p, "columns", 1)          // 1 = lista; 2–5 = cuadrícula
    val autoKeyboard = PrefBool(p, "auto_kb", true)
    val autoLaunch = PrefBool(p, "auto_launch", false)

    // Datos
    val favorites = PrefList(p, "favorites")
    val hidden = PrefList(p, "hidden")
    val renames = PrefMap(p, "renames")

    fun label(app: AppInfo): String = renames.value[app.key]?.takeIf { it.isNotBlank() } ?: app.label

    fun isFavorite(key: String) = key in favorites.value
    fun toggleFavorite(key: String) {
        favorites.value = if (isFavorite(key)) favorites.value - key else favorites.value + key
    }
    fun moveFavorite(key: String, delta: Int) {
        val l = favorites.value.toMutableList()
        val i = l.indexOf(key); val j = i + delta
        if (i < 0 || j !in l.indices) return
        l[i] = l[j].also { l[j] = l[i] }
        favorites.value = l
    }
    fun hide(key: String) {
        hidden.value = (hidden.value + key).distinct()
        favorites.value = favorites.value - key
    }
    fun unhide(key: String) { hidden.value = hidden.value - key }
    fun rename(key: String, name: String) {
        renames.value = if (name.isBlank()) renames.value - key else renames.value + (key to name.trim())
    }

    fun resetLook() {
        listOf(accent, font, align, columns).forEach { it.reset() }
        listOf(bgDim, textScale, clockSize, iconSize).forEach { it.reset() }
        listOf(clock24, showDate, iconsDrawer, iconsHome, autoKeyboard, autoLaunch).forEach { it.reset() }
    }

    // Derivados para la UI
    val fontFamily: FontFamily
        get() = when (font.value) { 2 -> FontFamily.Serif; 3 -> FontFamily.Monospace; else -> FontFamily.SansSerif }
    val bodyWeight: FontWeight
        get() = if (font.value == 1) FontWeight.Light else FontWeight.Normal
    val clockWeight: FontWeight
        get() = if (font.value == 1) FontWeight.Thin else FontWeight.Light
    val hAlign: Alignment.Horizontal
        get() = when (align.value) { 1 -> Alignment.CenterHorizontally; 2 -> Alignment.End; else -> Alignment.Start }
    val textAlign: TextAlign
        get() = when (align.value) { 1 -> TextAlign.Center; 2 -> TextAlign.End; else -> TextAlign.Start }
}
