package com.unfallen.nova.ui

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.DiaryEntry
import com.unfallen.nova.data.Moods
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val es = Locale.forLanguageTag("es-ES")
private val longDay = SimpleDateFormat("EEEE, d 'de' MMMM", es)
private val yearFmt = SimpleDateFormat("yyyy", es)
private val hourFmt = SimpleDateFormat("HH:mm", es)

private fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(es) else it.toString() }

private fun dayKey(t: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = t }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

private fun dayTitle(t: Long): String {
    val today = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(System.currentTimeMillis() - 86_400_000L)
    return when (dayKey(t)) {
        today -> "Hoy"
        yesterday -> "Ayer"
        else -> longDay.format(Date(t)).cap()
    }
}

/** Diario: formulario del día + entradas anteriores. Cada entrada se envía a la memoria de NOVA. */
@Composable
fun DiaryScreen(vm: NovaViewModel, onOpenCapsules: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    val now = System.currentTimeMillis()
    val entries = vm.diary.sortedByDescending { it.time }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Nova.Bg)
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        // ---------- Cabecera ----------
        item {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(top = 14.dp, bottom = 10.dp)
            ) {
                Text("Diario", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Lo que haces y lo que piensas. NOVA lo guarda en tu memoria para conocerte mejor.",
                    color = Nova.Muted, fontSize = 14.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.size(12.dp))
                // Acceso a las cápsulas del tiempo
                val readyCount = vm.readyCapsules()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Nova.Surface)
                        .border(1.dp, if (readyCount > 0) Nova.Purple else Nova.Stroke, RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenCapsules)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏳", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cápsulas del tiempo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                readyCount > 0 -> "Tienes $readyCount lista${if (readyCount == 1) "" else "s"} para abrir"
                                vm.capsules.isEmpty() -> "Escribe un mensaje a tu yo del futuro"
                                else -> "${vm.capsules.count { !it.opened }} sellada${if (vm.capsules.count { !it.opened } == 1) "" else "s"}"
                            },
                            color = if (readyCount > 0) Nova.Violet else Nova.Muted, fontSize = 13.sp
                        )
                    }
                    Text("›", color = Nova.Muted, fontSize = 24.sp)
                }
            }
        }

        // ---------- Formulario del día ----------
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1740), Color(0xFF101829))))
                    .border(1.dp, Color(0x557B5CFF), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        longDay.format(Date(now)).cap(),
                        color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(yearFmt.format(Date(now)), color = Nova.Muted, fontSize = 14.sp)
                }
                Spacer(Modifier.size(12.dp))
                Text("¿Cómo te sientes hoy?", color = Nova.Muted, fontSize = 13.sp)
                Spacer(Modifier.size(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Moods.all.forEach { m ->
                        Chip(m, selected = mood == m) { mood = if (mood == m) "" else m }
                    }
                }
                Spacer(Modifier.size(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1220))
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            "¿Qué has hecho hoy? ¿Qué te ha preocupado o ilusionado? Escribe lo que quieras o una reflexión…",
                            color = Nova.Muted, fontSize = 15.sp, lineHeight = 21.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(Nova.Purple),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (vm.isLearning) "🧠 NOVA está aprendiendo…" else "${text.trim().length} caracteres",
                        color = if (vm.isLearning) Nova.Violet else Nova.Muted, fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val canSave = text.isNotBlank()
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canSave) Nova.Purple else Color(0xFF2A2F45))
                            .clickable(enabled = canSave) {
                                vm.addDiaryEntry(text, mood)
                                text = ""
                                mood = ""
                            }
                            .padding(horizontal = 18.dp, vertical = 11.dp)
                    ) { Text("Guardar en mi diario", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                }
            }
        }

        // Aviso de "He aprendido…"
        item {
            vm.learnedNotice?.let {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { NoticeChip(it) }
            }
        }

        // ---------- Entradas anteriores ----------
        if (entries.isEmpty()) {
            item {
                Text(
                    "Aquí irán apareciendo tus días. Cada vez que escribas, NOVA sacará lo importante (qué te gusta, cómo te sientes, qué te preocupa) y lo añadirá a tu memoria y a tu retrato.",
                    color = Nova.Muted, fontSize = 14.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
        var lastDay = -1
        entries.forEach { e ->
            val k = dayKey(e.time)
            if (k != lastDay) {
                lastDay = k
                item(key = "day-$k") {
                    Text(
                        dayTitle(e.time),
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
                    )
                }
            }
            item(key = e.id) { DiaryCard(e, onDelete = { toDelete = e }) }
        }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar esta entrada?") },
            text = { Text("Se borra del diario. Lo que NOVA ya aprendió de ella sigue en Memoria, y ahí puedes borrarlo si quieres.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteDiaryEntry(e.id); toDelete = null }) { Text("Borrar", color = Nova.Danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

@Composable
private fun DiaryCard(e: DiaryEntry, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Nova.Surface)
            .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(start = 14.dp, top = 10.dp, bottom = 12.dp, end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hourFmt.format(Date(e.time)), color = Nova.Muted, fontSize = 12.sp)
            if (e.mood.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(e.mood, color = Nova.Text, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Delete, "Borrar entrada", tint = Nova.Muted, modifier = Modifier.size(18.dp)) }
        }
        Text(
            e.text,
            color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}
