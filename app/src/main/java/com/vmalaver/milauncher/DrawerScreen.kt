package com.vmalaver.milauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun DrawerScreen(
    s: LauncherSettings,
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val accent = Color(s.accent.value)
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val close by rememberUpdatedState(onClose)
    var query by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<AppInfo?>(null) }
    var drag by remember { mutableFloatStateOf(0f) }

    val results = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) apps
        else apps.map { it to normalize(s.label(it)) }
            .filter { (_, n) -> n.contains(q) || n.split(' ').joinToString("") { w -> w.take(1) }.startsWith(q) }
            .sortedBy { (_, n) -> if (n.startsWith(q)) 0 else 1 }
            .map { it.first }
    }

    val open: (AppInfo) -> Unit = { app ->
        keyboard?.hide()
        query = ""
        onLaunch(app)
    }

    LaunchedEffect(Unit) {
        if (s.autoKeyboard.value) {
            delay(200)
            runCatching { focus.requestFocus() }
        }
    }
    LaunchedEffect(results) {
        if (s.autoLaunch.value && query.isNotBlank() && results.size == 1) open(results[0])
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF00A0A0B))
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        // Cabecera: buscador + ajustes. Deslizar hacia abajo aquí cierra el cajón.
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = { if (drag > 100f) close(); drag = 0f },
                    ) { _, amount -> drag += amount }
                }
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text("Buscar apps", style = bodyStyle(s, 16f)) },
                textStyle = bodyStyle(s, 17f).copy(color = Color.White),
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, "Borrar", tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let(open) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = accent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                ),
            )
            IconButton(onClick = { keyboard?.hide(); onOpenSettings() }) {
                Icon(Icons.Filled.Settings, "Ajustes", tint = Color.White.copy(alpha = 0.7f))
            }
        }

        if (results.isEmpty()) {
            Text(
                "Sin resultados",
                modifier = Modifier.padding(16.dp),
                style = bodyStyle(s, 15f).copy(color = Color.White.copy(alpha = 0.4f)),
            )
        }

        val cols = s.columns.value
        if (cols <= 1) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(results, key = { it.key }) { app ->
                    AppEntry(s, app, grid = false, onClick = { open(app) }, onRename = { renaming = app })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.key }) { app ->
                    AppEntry(s, app, grid = true, onClick = { open(app) }, onRename = { renaming = app })
                }
            }
        }
    }

    renaming?.let { app -> RenameDialog(s, app) { renaming = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppEntry(
    s: LauncherSettings,
    app: AppInfo,
    grid: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val showIcon = s.iconsDrawer.value

    Box {
        val mod = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
        if (grid) {
            Column(mod.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showIcon) {
                    AppIcon(app.icon, s.iconSize.value)
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    s.label(app),
                    style = bodyStyle(s, 12f).copy(color = Color.White, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(
                mod.padding(horizontal = 8.dp, vertical = if (showIcon) 7.dp else 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (s.align.value) {
                    1 -> Arrangement.Center; 2 -> Arrangement.End; else -> Arrangement.Start
                },
            ) {
                if (showIcon) {
                    AppIcon(app.icon, s.iconSize.value)
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    s.label(app),
                    style = bodyStyle(s, 18f).copy(color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (s.isFavorite(app.key)) "Quitar de inicio" else "Fijar en inicio") },
                onClick = { s.toggleFavorite(app.key); menu = false },
            )
            DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; onRename() })
            DropdownMenuItem(text = { Text("Ocultar") }, onClick = { s.hide(app.key); menu = false })
            DropdownMenuItem(text = { Text("Información de la app") }, onClick = { Apps.openInfo(context, app); menu = false })
            DropdownMenuItem(text = { Text("Desinstalar") }, onClick = { Apps.uninstall(context, app); menu = false })
        }
    }
}

@Composable
private fun RenameDialog(s: LauncherSettings, app: AppInfo, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(s.label(app)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                supportingText = { Text("Nombre original: ${app.label}") },
            )
        },
        confirmButton = {
            TextButton(onClick = { s.rename(app.key, name); onDismiss() }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = { s.rename(app.key, ""); onDismiss() }) { Text("Restablecer") }
        },
    )
}
