package com.vmalaver.milauncher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
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
)

object Apps {
    private val iconCache = HashMap<String, ImageBitmap>()

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
                val icon = synchronized(iconCache) { iconCache[key] } ?: runCatching {
                    info.getBadgedIcon(0).toBitmap(144, 144).asImageBitmap()
                }.getOrNull()?.also { synchronized(iconCache) { iconCache[key] = it } } ?: continue
                result += AppInfo(key, info.label.toString(), cn, user, icon)
            }
        }
        return result
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
