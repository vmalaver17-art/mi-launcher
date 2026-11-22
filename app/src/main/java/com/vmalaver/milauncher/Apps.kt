package com.vmalaver.milauncher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Normalizer

data class AppInfo(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    /** Silueta del ícono (para pintarla en blanco, estilo Windows Phone). Puede faltar. */
    val glyph: ImageBitmap?,
)

object Apps {
    private val cache = HashMap<String, Pair<ImageBitmap, ImageBitmap?>>()

    /** Lee todas las apps instaladas que se pueden abrir (incluye perfil de trabajo). */
    fun load(context: Context): List<AppInfo> {
        val la = context.getSystemService(LauncherApps::class.java)
        val me = Process.myUserHandle()
        val result = ArrayList<AppInfo>()
        for (user in la.profiles) {
            for (info in la.getActivityList(null, user)) {
                val cn = info.componentName
                if (cn.packageName == context.packageName) continue
                val key = cn.flattenToString() + if (user == me) "" else "#${user.hashCode()}"
                val icons = synchronized(cache) { cache[key] } ?: runCatching {
                    val icon = info.getBadgedIcon(0).toBitmap(144, 144).asImageBitmap()
                    val glyph = runCatching { glyphOf(info.getIcon(0)) }.getOrNull()
                    icon to glyph
                }.getOrNull()?.also { synchronized(cache) { cache[key] = it } } ?: continue
                result += AppInfo(key, info.label.toString(), cn, user, icons.first, icons.second)
            }
        }
        return result
    }

    /** Toma la capa monocroma (Android 13+) o la capa frontal del ícono adaptativo. */
    private fun glyphOf(d: Drawable): ImageBitmap? {
        if (d !is AdaptiveIconDrawable) return null
        val mono = if (Build.VERSION.SDK_INT >= 33) d.monochrome else null
        val layer = (mono ?: d.foreground ?: return null).mutate()
        val size = 144
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pad = size / 4 // la zona visible del ícono adaptativo es el 66% central
        layer.setBounds(-pad, -pad, size + pad, size + pad)
        layer.draw(Canvas(bmp))
        return bmp.asImageBitmap()
    }

    fun launch(context: Context, app: AppInfo) {
        try {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInfo(context: Context, app: AppInfo) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(app.component, app.user, null, null)
        }
    }

    fun uninstall(context: Context, app: AppInfo) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openClock(context: Context) {
        runCatching {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        runCatching {
            val sb = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(sb)
        }
    }

    fun openHomeSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun pickWallpaper(context: Context) {
        runCatching {
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Fondo de pantalla")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

/** Minúsculas y sin tildes, para buscar "camara" y encontrar "Cámara". */
fun normalize(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
