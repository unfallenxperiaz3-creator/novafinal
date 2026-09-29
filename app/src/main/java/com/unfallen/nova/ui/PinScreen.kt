package com.unfallen.nova.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Candado de la pestaña Memoria.
 * - Si aún no hay PIN: lo crea (escribir y repetir).
 * - Si ya hay PIN: lo pide. 5 fallos = 30 s de espera.
 */
@Composable
fun PinLockScreen(vm: NovaViewModel, lockedTitle: String = "Memoria protegida", what: String = "lo que NOVA sabe de ti") {
    val creating = !vm.hasPin
    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }   // al crear: primer intento
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var lockLeft by remember { mutableLongStateOf(vm.pinLockRemaining()) }
    var lockTick by remember { mutableIntStateOf(0) }
    var askForget by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Cuenta atrás del bloqueo
    LaunchedEffect(lockTick) {
        lockLeft = vm.pinLockRemaining()
        while (lockLeft > 0) {
            delay(250)
            lockLeft = vm.pinLockRemaining()
        }
    }

    fun fail(text: String) {
        isError = true
        message = text
        entered = ""
        scope.launch {
            for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    fun submit(pin: String) {
        if (creating) {
            val first = firstPin
            if (first == null) {
                firstPin = pin
                entered = ""
                isError = false
                message = "Repítelo para confirmar"
            } else if (first == pin) {
                vm.createPin(pin)
            } else {
                firstPin = null
                fail("No coinciden. Vuelve a empezar.")
            }
        } else {
            when (vm.tryUnlock(pin)) {
                NovaViewModel.PinResult.OK -> Unit
                NovaViewModel.PinResult.WRONG -> fail("PIN incorrecto. Te quedan ${vm.pinAttemptsLeft()} intentos.")
                NovaViewModel.PinResult.LOCKED -> {
                    fail("Demasiados intentos.")
                    lockTick++
                }
            }
        }
    }

    val locked = lockLeft > 0
    val title = when {
        creating && firstPin == null -> "Crea tu PIN"
        creating -> "Confirma tu PIN"
        else -> lockedTitle
    }
    val subtitle = message ?: if (creating)
        "Elige 4 números. Protegerá tu Memoria y tu Diario."
    else "Introduce tu PIN para ver $what."

    Column(
        Modifier
            .fillMaxSize()
            .background(Nova.Bg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.6f))
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Nova.Purple, Nova.Blue))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            if (locked) "Demasiados intentos. Espera ${lockLeft / 1000 + 1} s." else subtitle,
            color = if (isError || locked) Nova.Danger else Nova.Muted,
            fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))

        // Puntos
        Row(
            Modifier.offset(x = shake.value.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            repeat(NovaViewModel.PIN_LENGTH) { i ->
                val filled = i < entered.length
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) Nova.Purple else Color.Transparent)
                        .border(2.dp, if (filled) Nova.Purple else Nova.Muted, CircleShape)
                )
            }
        }

        Spacer(Modifier.weight(0.4f))

        // Teclado numérico
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<"))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { key ->
                        PinKey(key, enabled = !locked) {
                            when (key) {
                                "<" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                "" -> Unit
                                else -> if (entered.length < NovaViewModel.PIN_LENGTH) {
                                    entered += key
                                    if (entered.length == NovaViewModel.PIN_LENGTH) submit(entered)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        if (!creating) {
            TextButton(onClick = { askForget = true }) {
                Text("¿Has olvidado el PIN?", color = Nova.Muted, fontSize = 13.sp)
            }
        } else if (firstPin != null) {
            TextButton(onClick = { firstPin = null; entered = ""; message = null; isError = false }) {
                Text("Empezar de nuevo", color = Nova.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (askForget) {
        AlertDialog(
            onDismissRequest = { askForget = false },
            title = { Text("¿Has olvidado el PIN?") },
            text = {
                Text(
                    "Para proteger lo que le has contado a NOVA, la única forma de quitar el PIN es borrar " +
                        "toda la memoria, tu retrato y el diario. La conversación del chat no se borra. No se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.forgetPinAndMemories(); askForget = false }) {
                    Text("Borrar todo y quitar PIN", color = Nova.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { askForget = false }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    val visible = label.isNotEmpty()
    Box(
        Modifier
            .size(68.dp)
            .clip(CircleShape)
            .then(
                if (visible) Modifier
                    .background(Nova.Surface)
                    .border(1.dp, Nova.Stroke, CircleShape)
                    .clickable(enabled = enabled, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "" -> Unit
            "<" -> Icon(
                Icons.Filled.Backspace, "Borrar",
                tint = if (enabled) Nova.Text else Nova.Muted,
                modifier = Modifier.size(24.dp)
            )
            else -> Text(
                label,
                color = if (enabled) Nova.Text else Nova.Muted,
                fontSize = 26.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}
