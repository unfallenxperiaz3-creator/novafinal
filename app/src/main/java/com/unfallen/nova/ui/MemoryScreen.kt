package com.unfallen.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.Categories
import com.unfallen.nova.data.Memory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES"))

/** Estado del diálogo de añadir/editar recuerdo. */
private data class MemoryDraft(val id: String?, val category: String, val text: String)

@Composable
fun MemoryScreen(vm: NovaViewModel) {
    val memories = vm.memories
    val profile = vm.profile
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf<MemoryDraft?>(null) }
    var editingProfile by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Memory?>(null) }

    val visible = memories
        .filter { filter == null || it.category == filter }
        .sortedByDescending { it.createdAt }
    val grouped = Categories.all.mapNotNull { cat ->
        val list = visible.filter { it.category == cat }
        if (list.isEmpty()) null else cat to list
    }

    Box(Modifier.fillMaxSize().background(Nova.Bg)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
        ) {
            // ---------- Cabecera ----------
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Memoria", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Lo que NOVA sabe de ti · ${memories.size} recuerdos",
                            color = Nova.Muted, fontSize = 14.sp
                        )
                    }
                }
                if (vm.isLearning) {
                    Text("🧠 Analizando la última conversación…", color = Nova.Violet, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            }

            // ---------- Retrato ----------
            item {
                Column(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF211A4A), Color(0xFF111A33))))
                        .border(1.dp, Color(0x557B5CFF), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, null, tint = Nova.Violet, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tu retrato", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        SmallIcon(Icons.Filled.Edit, "Editar retrato") { editingProfile = profile.text }
                        if (profile.text.isNotBlank()) {
                            SmallIcon(Icons.Filled.Delete, "Borrar retrato") { vm.setProfileText("") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        profile.text.ifBlank {
                            "Aún no te conozco lo suficiente. Háblame de tu día, de lo que te preocupa o de lo que te ilusiona, y aquí iré resumiendo cómo eres."
                        },
                        color = if (profile.text.isBlank()) Nova.Muted else Nova.Text,
                        fontSize = 14.sp, lineHeight = 20.sp
                    )
                    if (profile.updatedAt > 0) {
                        Text(
                            "Actualizado el ${dayFmt.format(Date(profile.updatedAt))}",
                            color = Nova.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // ---------- Filtros ----------
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Todas (${memories.size})", selected = filter == null) { filter = null }
                    Categories.all.forEach { cat ->
                        val n = memories.count { it.category == cat }
                        if (n > 0) Chip("${Categories.emoji(cat)} $cat ($n)", selected = filter == cat) { filter = cat }
                    }
                }
            }

            if (memories.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay recuerdos. NOVA los irá guardando sola mientras habláis, o puedes contarle algo directamente con el botón +.",
                        color = Nova.Muted, fontSize = 14.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }

            // ---------- Recuerdos por categoría ----------
            grouped.forEach { (cat, list) ->
                item(key = "h-$cat") {
                    Text(
                        "${Categories.emoji(cat)}  $cat",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                items(list, key = { it.id }) { m ->
                    MemoryRow(
                        m,
                        onEdit = { draft = MemoryDraft(m.id, m.category, m.text) },
                        onDelete = { toDelete = m }
                    )
                }
            }

            item {
                Text(
                    "🔒 Tus recuerdos se guardan solo en este móvil. Para poder responderte, NOVA los envía a OpenAI junto con cada mensaje.",
                    color = Nova.Muted, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }

        // Botón +
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Nova.Purple, Nova.Blue)))
                .clickable { draft = MemoryDraft(null, filter ?: Categories.SOBRE_MI, "") },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, "Contarle algo a NOVA", tint = Color.White, modifier = Modifier.size(28.dp)) }
    }

    // ---------- Diálogos ----------
    draft?.let { d ->
        MemoryDialog(
            initial = d,
            onDismiss = { draft = null },
            onSave = { cat, text ->
                if (d.id == null) vm.addMemory(cat, text) else vm.editMemory(d.id, cat, text)
                draft = null
            }
        )
    }

    editingProfile?.let { current ->
        var text by remember { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { editingProfile = null },
            title = { Text("Tu retrato") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    minLines = 4, maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setProfileText(text); editingProfile = null }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editingProfile = null }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }

    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Olvidar este recuerdo?") },
            text = { Text("“${m.text}”") },
            confirmButton = {
                TextButton(onClick = { vm.deleteMemory(m.id); toDelete = null }) { Text("Olvidar", color = Nova.Danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }

}

@Composable
private fun MemoryRow(m: Memory, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Nova.Surface)
            .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(m.text, color = Nova.Text, fontSize = 15.sp, lineHeight = 20.sp)
            Text(
                dayFmt.format(Date(m.createdAt)) + if (m.manual) " · lo añadiste tú" else " · aprendido en una charla",
                color = Nova.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)
            )
        }
        SmallIcon(Icons.Filled.Delete, "Borrar", onClick = onDelete)
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = Nova.Muted, modifier = Modifier.size(20.dp)) }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Nova.Muted,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Nova.Purple else Nova.Surface)
            .border(1.dp, if (selected) Nova.Purple else Nova.Stroke, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun MemoryDialog(
    initial: MemoryDraft,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var category by remember { mutableStateOf(initial.category) }
    var text by remember { mutableStateOf(initial.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Cuéntale algo a NOVA" else "Editar recuerdo") },
        text = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Categories.all.forEach { cat ->
                        Chip("${Categories.emoji(cat)} $cat", selected = category == cat) { category = cat }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ej.: Me relaja conducir de noche con música") },
                    minLines = 3, maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(category, text) }, enabled = text.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = Nova.Surface
    )
}
