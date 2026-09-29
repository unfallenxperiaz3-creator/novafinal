package com.unfallen.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.Storage
import java.util.Locale

@Composable
fun SettingsScreen(vm: NovaViewModel) {
    var name by remember { mutableStateOf(vm.userName) }
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.model) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Nova.Bg)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text("Ajustes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        // ---------- Tú ----------
        Section("Sobre ti") {
            OutlinedTextField(
                value = name, onValueChange = { name = it; saved = false },
                label = { Text("¿Cómo quieres que te llame?") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- IA ----------
        Section("Conexión con la IA") {
            OutlinedTextField(
                value = key, onValueChange = { key = it; saved = false },
                label = { Text("OpenAI API key") },
                placeholder = { Text("sk-…") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar clave")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it; saved = false },
                label = { Text("Modelo") },
                placeholder = { Text(Storage.DEFAULT_MODEL) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text(
                "La clave se guarda solo en este móvil. Consíguela en platform.openai.com → API keys.",
                color = Nova.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    vm.updateUserName(name)
                    vm.updateApiKey(key)
                    vm.updateModel(model)
                    model = vm.model
                    saved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Nova.Purple),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saved) "Guardado ✓" else "Guardar") }
        }

        // ---------- Voz ----------
        Section("Voz") {
            ToggleRow("Respuestas habladas", "NOVA lee en voz alta lo que contesta", vm.voiceEnabled) { vm.updateVoiceEnabled(it) }
            ToggleRow("Conversación continua", "En la pestaña Voz, vuelve a escucharte al terminar de hablar", vm.handsFree) { vm.updateHandsFree(it) }
            Spacer(Modifier.height(6.dp))
            Text("Velocidad: ${fmt(vm.speechRate)}x", color = Nova.Text, fontSize = 14.sp)
            Slider(
                value = vm.speechRate, onValueChange = { vm.updateRate(it) },
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(thumbColor = Nova.Purple, activeTrackColor = Nova.Purple)
            )
            Text("Tono: ${fmt(vm.speechPitch)}", color = Nova.Text, fontSize = 14.sp)
            Slider(
                value = vm.speechPitch, onValueChange = { vm.updatePitch(it) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(thumbColor = Nova.Purple, activeTrackColor = Nova.Purple)
            )
            OutlinedButton(onClick = { vm.testVoice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Probar voz", color = Nova.Text)
            }
            Text(
                "Si suena rara, en Ajustes del móvil → Idioma → Salida de texto a voz, instala la voz en español de Google.",
                color = Nova.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
            )
        }

        // ---------- Memoria ----------
        Section("Memoria") {
            ToggleRow(
                "Aprender de mis conversaciones",
                "Tras cada respuesta, NOVA analiza lo que le cuentas y guarda lo importante",
                vm.learningEnabled
            ) { vm.updateLearning(it) }
        }

        // ---------- Seguridad ----------
        Section("Seguridad") {
            PinSettings(vm)
        }

        // ---------- Conversación ----------
        Section("Conversación") {
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Borrar historial de mensajes", color = Nova.Danger)
            }
        }

        Text(
            "NOVA 2.0 · ${vm.memories.size} recuerdos · ${vm.messages.size} mensajes",
            color = Nova.Muted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp).align(Alignment.CenterHorizontally)
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar los mensajes?") },
            text = { Text("Los recuerdos se mantienen.") },
            confirmButton = { TextButton(onClick = { vm.clearChat(); confirmClear = false }) { Text("Borrar", color = Nova.Danger) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

private fun fmt(v: Float) = String.format(Locale.US, "%.1f", v)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Nova.Bg2)
            .border(1.dp, Nova.Stroke, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Nova.Violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Nova.Text, fontSize = 15.sp)
            Text(subtitle, color = Nova.Muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Nova.Purple)
        )
    }
}

/** Crear o cambiar el PIN que protege la pestaña Memoria. */
@Composable
private fun PinSettings(vm: NovaViewModel) {
    var editing by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    fun reset() { current = ""; newPin = ""; repeat = ""; error = null }
    val digits = { v: String -> v.filter { it.isDigit() }.take(NovaViewModel.PIN_LENGTH) }

    Text(
        if (vm.hasPin) "La pestaña Memoria está protegida con un PIN de 4 números."
        else "La pestaña Memoria aún no tiene PIN. Se te pedirá crearlo al entrar en ella.",
        color = Nova.Muted, fontSize = 13.sp
    )
    if (done) {
        Text("PIN guardado ✓", color = Nova.Success, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
    }
    if (!editing) {
        OutlinedButton(
            onClick = { editing = true; done = false; reset() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text(if (vm.hasPin) "Cambiar PIN" else "Crear PIN", color = Nova.Text) }
        return
    }

    if (vm.hasPin) PinField(current, { current = digits(it) }, "PIN actual")
    PinField(newPin, { newPin = digits(it) }, "PIN nuevo (4 números)")
    PinField(repeat, { repeat = digits(it) }, "Repite el PIN nuevo")
    error?.let { Text(it, color = Nova.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { editing = false; reset() }) { Text("Cancelar", color = Nova.Muted) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val e = vm.changePin(current, newPin, repeat)
                if (e == null) { editing = false; done = true; reset() } else error = e
            },
            colors = ButtonDefaults.buttonColors(containerColor = Nova.Purple)
        ) { Text("Guardar PIN") }
    }
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}
