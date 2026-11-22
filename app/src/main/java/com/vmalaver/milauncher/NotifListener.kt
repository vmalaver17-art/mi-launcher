package com.vmalaver.milauncher

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.app.NotificationManagerCompat

/** Cuenta las notificaciones de cada app para mostrarlas en los mosaicos. */
object Badges {
    val counts = mutableStateMapOf<String, Int>()

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun requestAccess(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        instance = null
        Badges.counts.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    fun refresh() {
        val list = runCatching { activeNotifications }.getOrNull() ?: return
        val map = HashMap<String, Int>()
        for (n in list) {
            if (n.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            if (n.isOngoing) continue // música, descargas, etc.
            map[n.packageName] = (map[n.packageName] ?: 0) + 1
        }
        Badges.counts.keys.filter { it !in map }.forEach { Badges.counts.remove(it) }
        Badges.counts.putAll(map)
    }

    companion object {
        var instance: NotifListener? = null
    }
}
