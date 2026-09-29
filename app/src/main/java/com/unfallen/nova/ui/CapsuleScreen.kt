package com.unfallen.nova.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.TimeCapsule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val capDate = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))

/** El día elegido, a las 10:00 de la mañana. */
private fun at10(cal: Calendar): Long = (cal.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun inMonths(n: Int): Long = at10(Calendar.getInstance().apply { add(Calendar.MONTH, n) })

private fun daysUntil(t: Long): Long =
    ((t - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L) + 1

@Composable
fun CapsuleScreen(vm: NovaViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var openAt by remember { mutableLongStateOf(inMonths(6)) }
    var choice by rememberSaveable { mutableStateOf("6 meses") }
    var toDelete by remember { mutableStateOf<TimeCapsule?>(null) }
    var sealedNotice by remember { mutableStateOf<String?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun seal() {
        vm.addCapsule(text, openAt)
        sealedNotice = "🔒 Cápsula sellada. Te la entregaré el ${capDate.format(Date(openAt))}."
        text = ""
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val now = System.currentTimeMillis()
    val ready = vm.capsules.filter { it.isReady(now) && !it.opened }.sortedBy { it.openAt }
    val sealed = vm.capsules.filter { !it.isReady(now) }.sortedBy { it.openAt }
    val opened = vm.capsules.filter { it.opened }.sortedByDescending { it.openAt }

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
                Text("Cápsulas del tiempo", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Escribe a tu yo del futuro. Quedará sellada y NOVA te la entregará el día que elijas, con un comentario sobre cómo has cambiado.",
                color = Nova.Muted, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // ---------- Nueva cápsula ----------
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF231A45), Color(0xFF0F1A2E))))
                    .border(1.dp, Color(0x557B5CFF), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1220))
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            "Querido yo del futuro… ¿Qué te gustaría haber conseguido? ¿Qué te preocupa hoy? ¿Qué no quieres olvidar?",
                            color = Nova.Muted, fontSize = 15.sp, lineHeight = 21.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(Nova.Purple),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 122.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text("¿Cuándo quieres abrirla?", color = Nova.Muted, fontSize = 13.sp)
                Spacer(Modifier.size(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1 mes" to 1, "3 meses" to 3, "6 meses" to 6, "1 año" to 12).forEach { (label, months) ->
                        Chip(label, selected = choice == label) {
                            choice = label
                            openAt = inMonths(months)
                        }
                    }
                    Chip("📅 Elegir fecha", selected = choice == "custom") {
                        val c = Calendar.getInstance().apply { timeInMillis = openAt }
                        val dlg = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = Calendar.getInstance().apply { set(y, m, d) }
                                openAt = at10(picked)
                                choice = "custom"
                            },
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                        )
                        dlg.datePicker.minDate = System.currentTimeMillis() + 86_400_000L
                        dlg.show()
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "Se abrirá el ${capDate.format(Date(openAt))}",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.size(12.dp))
                val canSeal = text.isNotBlank()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSeal) Nova.Purple else Color(0xFF2A2F45))
                        .clickable(enabled = canSeal) { seal() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Sellar cápsula 🔒", color = Color.White, fontWeight = FontWeight.SemiBold) }
                sealedNotice?.let {
                    Text(it, color = Nova.Success, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ---------- Listas para abrir ----------
        if (ready.isNotEmpty()) {
            item { SectionTitle("📬 Listas para abrir") }
            items(ready, key = { it.id }) { c ->
                Column(
                    Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Nova.PurpleDeep, Nova.Blue)))
                        .clickable { vm.openCapsule(c.id) }
                        .padding(16.dp)
                ) {
                    Text("Te la escribiste el ${capDate.format(Date(c.createdAt))}", color = Color.White, fontSize = 14.sp)
                    Text("Toca para abrirla", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ---------- Abiertas ----------
        if (opened.isNotEmpty()) {
            item { SectionTitle("✉️ Abiertas") }
            items(opened, key = { it.id }) { c ->
                Column(
                    Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Nova.Surface)
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(18.dp))
                        .padding(start = 14.dp, top = 10.dp, bottom = 14.dp, end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Escrita el ${capDate.format(Date(c.createdAt))} · abierta el ${capDate.format(Date(c.openAt))}",
                            color = Nova.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f)
                        )
                        DeleteDot { toDelete = c }
                    }
                    Text(c.text, color = Nova.Text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(end = 10.dp, top = 4.dp))
                    when {
                        c.novaNote.isNotBlank() -> Column(
                            Modifier
                                .padding(top = 12.dp, end = 10.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1740))
                                .padding(12.dp)
                        ) {
                            Text("👽 NOVA", color = Nova.Violet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(c.novaNote, color = Nova.Text, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                        vm.generatingNote == c.id -> Row(
                            Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThinkingDots()
                            Spacer(Modifier.width(8.dp))
                            Text("NOVA está leyendo tu cápsula…", color = Nova.Muted, fontSize = 13.sp)
                        }
                        vm.hasApiKey -> TextButton(onClick = { vm.openCapsule(c.id) }) {
                            Text("Pedir a NOVA su comentario", color = Nova.Violet)
                        }
                    }
                }
            }
        }

        // ---------- Selladas ----------
        if (sealed.isNotEmpty()) {
            item { SectionTitle("🔒 Selladas") }
            items(sealed, key = { it.id }) { c ->
                Row(
                    Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Nova.Surface)
                        .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Nova.Violet, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Se abrirá el ${capDate.format(Date(c.openAt))}", color = Nova.Text, fontSize = 15.sp)
                        Text(
                            "Faltan ${daysUntil(c.openAt)} días · escrita el ${capDate.format(Date(c.createdAt))}",
                            color = Nova.Muted, fontSize = 12.sp
                        )
                    }
                    DeleteDot { toDelete = c }
                }
            }
        }

        if (vm.capsules.isEmpty()) {
            item {
                Text(
                    "Aún no tienes cápsulas. Escribe la primera: dentro de unos meses te hará ilusión leerla.",
                    color = Nova.Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar esta cápsula?") },
            text = {
                Text(
                    if (c.opened) "Se borrará la cápsula y el comentario de NOVA."
                    else "Se borrará sin abrir y no llegará nunca. No se puede deshacer."
                )
            },
            confirmButton = { TextButton(onClick = { vm.deleteCapsule(c.id); toDelete = null }) { Text("Borrar", color = Nova.Danger) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun DeleteDot(onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Filled.Delete, "Borrar cápsula", tint = Nova.Muted, modifier = Modifier.size(18.dp)) }
}
