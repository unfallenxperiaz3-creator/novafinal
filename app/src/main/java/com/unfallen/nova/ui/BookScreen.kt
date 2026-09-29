package com.unfallen.nova.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.unfallen.nova.NovaViewModel
import java.io.File

/** Tarjeta del Diario que abre el creador de "El libro de tu vida". */
@Composable
fun BookCard(vm: NovaViewModel) {
    var showBook by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Nova.Surface)
            .border(1.dp, Nova.Stroke, RoundedCornerShape(16.dp))
            .clickable { showBook = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📖", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("El libro de tu vida", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Tu diario, sueños y cartas en un PDF para guardar", color = Nova.Muted, fontSize = 13.sp)
        }
        Text("›", color = Nova.Muted, fontSize = 24.sp)
    }
    if (showBook) BookDialog(vm) { showBook = false }
}

@Composable
private fun BookDialog(vm: NovaViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    var includeDreams by remember { mutableStateOf(true) }
    var prologue by remember { mutableStateOf(vm.hasApiKey) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    val file = vm.bookFile
    val busy = vm.bookBusy
    val nothing = vm.diary.isEmpty() && vm.dreams.isEmpty() && vm.memories.isEmpty() && vm.profile.text.isBlank()

    AlertDialog(
        onDismissRequest = { if (busy == null) onClose() },
        title = { Text("📖 El libro de tu vida") },
        text = {
            Column {
                Text(
                    "Crea un PDF con aspecto de libro: portada con NOVA, tu retrato, lo que sabe de ti, tu diario día a día" +
                        ", tus sueños y las cartas de tus cápsulas ya abiertas.",
                    fontSize = 14.sp, color = Nova.Text
                )
                Spacer(Modifier.size(12.dp))
                if (nothing) {
                    Text("Aún no hay nada que poner en el libro. Escribe en tu diario primero.", color = Nova.Muted, fontSize = 13.sp)
                } else {
                    BookToggle("Incluir sueños", includeDreams) { includeDreams = it }
                    BookToggle(
                        if (vm.hasApiKey) "Prólogo escrito por NOVA" else "Prólogo de NOVA (necesita API key)",
                        prologue && vm.hasApiKey
                    ) { if (vm.hasApiKey) prologue = it }
                }
                if (busy != null) {
                    Spacer(Modifier.size(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ThinkingDots()
                        Spacer(Modifier.width(8.dp))
                        Text(busy, color = Nova.Muted, fontSize = 13.sp)
                    }
                }
                if (file != null && busy == null) {
                    Spacer(Modifier.size(12.dp))
                    Text("✓ Libro listo", color = Nova.Success, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.size(6.dp))
                    BookAction("Abrir") { openPdf(context, file) }
                    BookAction("Guardar en Descargas") {
                        savedMsg = if (vm.saveBookToDownloads()) "Guardado en la carpeta Descargas como ${file.name}"
                        else "No he podido guardarlo aquí. Usa Compartir y elige \"Guardar en Archivos\" o Drive."
                    }
                    BookAction("Compartir / enviar") { sharePdf(context, file) }
                    savedMsg?.let { Text(it, color = Nova.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        },
        confirmButton = {
            if (!nothing) {
                TextButton(
                    onClick = { savedMsg = null; vm.createBook(includeDreams, prologue && vm.hasApiKey) },
                    enabled = busy == null
                ) { Text(if (file == null) "Crear libro" else "Volver a crear") }
            }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = busy == null) { Text("Cerrar") } },
        containerColor = Nova.Surface
    )
}

@Composable
private fun BookToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Nova.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Nova.Purple))
    }
}

@Composable
private fun BookAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Nova.PurpleDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

private fun uriFor(context: Context, file: File) =
    FileProvider.getUriForFile(context, "com.unfallen.nova.files", file)

private fun openPdf(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriFor(context, file), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Abrir libro con…"))
    } catch (e: ActivityNotFoundException) {
        sharePdf(context, file)
    }
}

private fun sharePdf(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartir libro"))
    } catch (e: ActivityNotFoundException) {
    }
}
