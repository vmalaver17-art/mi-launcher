@file:OptIn(ExperimentalFoundationApi::class)

package com.vmalaver.milauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val LETTERS = "#ABCDEFGHIJKLMNÑOPQRSTUVWXYZ"

private fun letterOf(name: String): Char {
    val c = normalize(name).firstOrNull()?.uppercaseChar() ?: return '#'
    return if (c in 'A'..'Z') c else if (name.firstOrNull()?.uppercaseChar() == 'Ñ') 'Ñ' else '#'
}

private sealed interface Row_ {
    data class Header(val letter: Char) : Row_
    data class Entry(val app: AppInfo) : Row_
}

@Composable
fun AppListScreen(
    s: LauncherSettings,
    apps: List<AppInfo>,
    active: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val accent = Color(s.accent.value)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<AppInfo?>(null) }
    var letterGrid by remember { mutableStateOf(false) }

    // Al salir de esta página: cerrar teclado y limpiar búsqueda
    LaunchedEffect(active) {
        if (!active) {
            keyboard?.hide(); focusManager.clearFocus(); query = ""; letterGrid = false
            listState.scrollToItem(0)
        }
    }

    val rows: List<Row_> = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isNotEmpty()) {
            apps.filter { normalize(s.label(it)).contains(q) }
                .sortedBy { if (normalize(s.label(it)).startsWith(q)) 0 else 1 }
                .map { Row_.Entry(it) }
        } else {
            val out = ArrayList<Row_>()
            var last: Char? = null
            apps.sortedWith(compareBy({ LETTERS.indexOf(letterOf(s.label(it))) }, { normalize(s.label(it)) }))
                .forEach { app ->
                    val l = letterOf(s.label(app))
                    if (l != last) { out += Row_.Header(l); last = l }
                    out += Row_.Entry(app)
                }
            out
        }
    }
    val present = remember(rows) { rows.filterIsInstance<Row_.Header>().map { it.letter }.toSet() }

    val open: (AppInfo) -> Unit = { app ->
        keyboard?.hide(); focusManager.clearFocus(); query = ""
        onLaunch(app)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("buscar", style = bodyStyle(s, 17f)) },
                    textStyle = bodyStyle(s, 17f).copy(color = Color.White),
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, "Borrar", tint = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        rows.firstNotNullOfOrNull { (it as? Row_.Entry)?.app }?.let(open)
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = accent,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
                    ),
                )
                IconButton(onClick = { keyboard?.hide(); onOpenSettings() }) {
                    Icon(Icons.Filled.Settings, "Personalizar", tint = Color.White.copy(alpha = 0.8f))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(rows, key = { r -> if (r is Row_.Header) "h" + r.letter else "a" + (r as Row_.Entry).app.key }) { r ->
                    when (r) {
                        is Row_.Header -> LetterHeader(s, r.letter) { keyboard?.hide(); letterGrid = true }
                        is Row_.Entry -> AppRow(s, r.app, onClick = { open(r.app) }, onRename = { renaming = r.app })
                    }
                }
            }
        }

        // Cuadrícula de letras para saltar (como en Windows Phone)
        if (letterGrid) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { letterGrid = false }
                    .systemBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(LETTERS.toList()) { l ->
                        val has = l in present
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .background(if (has) accent else Color(0xFF2A2A2A))
                                .clickable(enabled = has) {
                                    val idx = rows.indexOfFirst { it is Row_.Header && it.letter == l }
                                    letterGrid = false
                                    if (idx >= 0) scope.launch { listState.scrollToItem(idx) }
                                },
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Text(
                                l.lowercase(),
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                                style = bodyStyle(s, 30f).copy(color = if (has) Color.White else Color.White.copy(alpha = 0.3f)),
                            )
                        }
                    }
                }
            }
        }
    }

    renaming?.let { app -> RenameDialog(s, app) { renaming = null } }
}

@Composable
private fun LetterHeader(s: LauncherSettings, letter: Char, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(50.dp)
            .border(2.dp, Color(s.accent.value))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            letter.lowercase(),
            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
            style = bodyStyle(s, 26f).copy(color = Color(s.accent.value)),
        )
    }
}

@Composable
private fun AppRow(s: LauncherSettings, app: AppInfo, onClick: () -> Unit, onRename: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(50.dp).background(Color(s.colorOf(app.key)).copy(alpha = s.tileAlpha.value)),
                contentAlignment = Alignment.Center,
            ) {
                TileIcon(s, app, Modifier.size(32.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                s.label(app),
                style = bodyStyle(s, 20f).copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (s.isPinned(app.key)) "Desanclar de inicio" else "Anclar a inicio") },
                onClick = { s.togglePin(app.key); menu = false },
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
                supportingText = { Text("Nombre original: ${app.label}", fontSize = 12.sp) },
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
