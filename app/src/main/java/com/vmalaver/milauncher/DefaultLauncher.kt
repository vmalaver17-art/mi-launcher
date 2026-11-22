package com.vmalaver.milauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** ¿Mi Launcher es la pantalla de inicio actual? */
fun isDefaultLauncher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = context.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val info = context.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY,
    )
    return info?.activityInfo?.packageName == context.packageName
}

private fun tryStart(context: Context, intent: Intent): Boolean =
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

/** Abre la pantalla de ajustes donde se elige la app de inicio (varía según la marca del celular). */
private fun openDefaultSettings(context: Context) {
    if (tryStart(context, Intent(Settings.ACTION_HOME_SETTINGS))) return
    if (tryStart(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) return
    tryStart(context, Intent(Settings.ACTION_SETTINGS))
}

/**
 * Devuelve una acción que pide a Android poner Mi Launcher como pantalla de inicio.
 * En Android 10+ muestra el aviso del sistema; si no se puede, abre los ajustes correspondientes.
 */
@Composable
fun rememberSetDefaultLauncher(): () -> Unit {
    val context = LocalContext.current
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Si el aviso no apareció o se canceló, mandar a los ajustes
        if (!isDefaultLauncher(context)) openDefaultSettings(context)
    }
    return remember(context, request) {
        {
            var shown = false
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    shown = runCatching { request.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }.isSuccess
                }
            }
            if (!shown) openDefaultSettings(context)
        }
    }
}
