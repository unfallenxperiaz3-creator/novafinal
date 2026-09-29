package com.unfallen.nova.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.Dream
import com.unfallen.nova.data.DreamFeelings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dreamDay = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))
private fun String.capFirst() = replaceFirstChar { it.uppercaseChar() }

/** Diario de sueños: escribes el sueño y NOVA lo interpreta en el momento buscando en internet. */
@Composable
fun DreamScreen(vm: NovaViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var text by rememberSaveable { mutableStateOf("") }
    var feeling by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Dream?>(null) }
    val list = vm.dreams.sortedByDescending { it.time }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Nova.Bg)
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ArrowBack, "Volver al diario", tint = Color.White) }
                Spacer(Modifier.width(6.dp))
                Text("Diario de sueños", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Apunta lo que has soñado nada más despertar. NOVA busca en internet qué suele significar y te lo explica.",
                color = Nova.Muted, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // ---------- Nuevo sueño ----------
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF14123A), Color(0xFF0B1630))))
                    .border(1.dp, Color(0x5555C7FF), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Text("🌙 Sueño del ${dreamDay.format(Date())}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(10.dp))
                Text("¿Cómo te has despertado?", color = Nova.Muted, fontSize = 13.sp)
                Spacer(Modifier.size(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DreamFeelings.all.forEach { f -> Chip(f, selected = feeling == f) { feeling = if (feeling == f) "" else f } }
                }
                Spacer(Modifier.size(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B0F1C))
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            "Cuéntalo con todos los detalles que recuerdes: dónde estabas, quién salía, qué pasaba, colores, sensaciones…",
                            color = Nova.Muted, fontSize = 15.sp, lineHeight = 21.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(Nova.Cyan),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                val canSend = text.isNotBlank()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) Brush.linearGradient(listOf(Nova.PurpleDeep, Nova.Blue)) else SolidColor(Color(0xFF2A2F45)))
                        .clickable(enabled = canSend) {
                            vm.addDream(text, feeling)
                            text = ""
                            feeling = ""
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Interpretar mi sueño 🌙", color = Color.White, fontWeight = FontWeight.SemiBold) }
                if (!vm.hasApiKey) {
                    Text(
                        "Para interpretarlo necesito tu API key (Ajustes). El sueño se guardará igualmente.",
                        color = Nova.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (list.isEmpty()) {
            item {
                Text(
                    "Aún no has apuntado ningún sueño. Con el tiempo, NOVA notará qué temas se repiten en tus sueños.",
                    color = Nova.Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else {
            item {
                Text(
                    "Tus sueños (${list.size})",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
                )
            }
        }

        items(list, key = { it.id }) { d ->
            DreamCard(
                d,
                loading = d.id in vm.interpreting,
                onRetry = { vm.interpretDream(d.id) },
                onDelete = { toDelete = d }
            )
        }
    }

    toDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar este sueño?") },
            text = { Text("Se borrará el sueño y su interpretación.") },
            confirmButton = { TextButton(onClick = { vm.deleteDream(d.id); toDelete = null }) { Text("Borrar", color = Nova.Danger) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

@Composable
private fun DreamCard(d: Dream, loading: Boolean, onRetry: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(
        Modifier
            .padding(vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Nova.Surface)
            .border(1.dp, Nova.Stroke, RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(start = 14.dp, top = 10.dp, bottom = 14.dp, end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dreamDay.format(Date(d.time)).capFirst() + if (d.feeling.isNotBlank()) " · ${d.feeling}" else "",
                color = Nova.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Delete, "Borrar sueño", tint = Nova.Muted, modifier = Modifier.size(18.dp)) }
        }
        Text(
            d.text,
            color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 10.dp).clickable { expanded = !expanded }
        )

        Column(
            Modifier
                .padding(top = 12.dp, end = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF15183A))
                .padding(12.dp)
        ) {
            Text("👽 Lo que dice NOVA", color = Nova.Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(4.dp))
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    ThinkingDots()
                    Spacer(Modifier.width(8.dp))
                    Text("Buscando en internet qué significa…", color = Nova.Muted, fontSize = 13.sp)
                }
                d.interpretation.isNotBlank() -> {
                    Text(d.interpretation, color = Nova.Text, fontSize = 14.sp, lineHeight = 20.sp)
                    if (d.sources.isNotEmpty()) {
                        Spacer(Modifier.size(10.dp))
                        Text("Fuentes", color = Nova.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        d.sources.forEach { s ->
                            Text(
                                s.title,
                                color = Nova.Cyan, fontSize = 13.sp,
                                textDecoration = TextDecoration.Underline,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { runCatching { uri.openUri(s.url) } }
                            )
                        }
                    } else if (!d.searched) {
                        Text(
                            "Interpretado sin búsqueda en internet.",
                            color = Nova.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (d.failed) "No he podido interpretarlo ahora." else "Sin interpretar todavía.",
                        color = Nova.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text("Interpretar", color = Nova.Cyan) }
                }
            }
        }
    }
}
