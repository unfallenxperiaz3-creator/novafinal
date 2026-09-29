package com.unfallen.nova.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.NovaStatus

fun statusLabel(status: NovaStatus, idle: String = "Tu IA personal"): String = when (status) {
    NovaStatus.IDLE -> idle
    NovaStatus.LISTENING -> "Escuchando…"
    NovaStatus.THINKING -> "Pensando…"
    NovaStatus.SPEAKING -> "Hablando…"
}

@Composable
fun NovaHeader(
    status: NovaStatus,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 14.dp, top = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text("NOVA", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(statusLabel(status), color = Color(0xE6FFFFFF), fontSize = 15.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) { actions() }
    }
}

@Composable
fun ChatScreen(
    vm: NovaViewModel,
    onMic: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val messages = vm.messages
    val status = vm.status
    var input by rememberSaveable { mutableStateOf("") }
    var bigAvatar by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val thinking = status == NovaStatus.THINKING
    val listening = status == NovaStatus.LISTENING
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Baja solo al último mensaje
    LaunchedEffect(messages.size, thinking) {
        val count = messages.size + (if (thinking) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun submit() {
        val t = input.trim()
        if (t.isNotEmpty() && !thinking) {
            vm.send(t)
            input = ""
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Nova.Bg)) {
        val full = maxHeight
        val empty = messages.isEmpty()
        val stageTarget = when {
            empty -> full
            imeOpen -> full * 0.2f
            bigAvatar -> full * 0.58f
            else -> full * 0.38f
        }
        val stageHeight by animateDpAsState(stageTarget, tween(500), label = "stage")

        // ---------- Avatar arriba ----------
        AlienAvatar(
            status = status,
            wordTick = vm.wordTick,
            micLevel = vm.micLevel,
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight)
                .clickable(enabled = status == NovaStatus.SPEAKING) { vm.stopSpeaking() }
        )

        // Onda sobre el pecho del alien mientras escucha / habla
        if (listening || status == NovaStatus.SPEAKING) {
            Waveform(
                level = if (listening) vm.micLevel else 0.55f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (stageHeight * 0.66f).coerceAtLeast(120.dp))
                    .fillMaxWidth(0.72f)
                    .height(if (empty) 90.dp else 56.dp)
            )
        }

        NovaHeader(status) {
            RoundIcon(
                icon = if (vm.voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                description = "Voz",
                onClick = { vm.updateVoiceEnabled(!vm.voiceEnabled) }
            )
            if (!empty) {
                RoundIcon(
                    icon = if (bigAvatar) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                    description = "Tamaño del avatar",
                    onClick = { bigAvatar = !bigAvatar }
                )
                RoundIcon(
                    icon = Icons.Filled.DeleteSweep,
                    description = "Borrar conversación",
                    onClick = { confirmClear = true }
                )
            }
        }

        Column(Modifier.fillMaxSize().imePadding()) {
            if (empty) {
                Spacer(Modifier.weight(1f))
                // Bienvenida + botón grande de hablar (pantalla 1 del diseño)
                GlassCard(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 36.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (listening) "Te estoy escuchando…"
                            else if (vm.userName.isNotBlank()) "Hola ${vm.userName}, soy NOVA" else "Hola, soy NOVA",
                            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (listening && vm.partialText.isNotBlank()) vm.partialText
                            else "Estoy aquí para hablar contigo, conocerte y acompañarte.",
                            color = Nova.Muted, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }
                }
                if (!imeOpen) {
                    Spacer(Modifier.height(22.dp))
                    MicOrb(
                        listening = listening,
                        level = vm.micLevel,
                        onClick = onMic,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        if (listening) "Toca para enviar" else "Toca para hablar",
                        color = Color.White, fontSize = 15.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp, bottom = 14.dp)
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                Spacer(Modifier.height(stageHeight - 28.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    items(messages, key = { it.id }) { m ->
                        ChatBubble(
                            msg = m,
                            isSpeaking = vm.speakingId == m.id,
                            onSpeak = { vm.speak(m) },
                            onStop = { vm.stopSpeaking() }
                        )
                    }
                    if (thinking) {
                        item(key = "thinking") {
                            Row(
                                Modifier
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xE6141A29))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) { ThinkingDots() }
                        }
                    }
                }
            }

            // Avisos
            vm.learnedNotice?.let {
                NoticeChip(it, Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp, vertical = 4.dp))
            }
            vm.error?.let { err ->
                ErrorBanner(
                    text = err,
                    onClose = { vm.dismissError() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    actionLabel = if (!vm.hasApiKey) "Ajustes" else null,
                    onAction = onOpenSettings
                )
            }

            // ---------- Barra de entrada ----------
            if (listening) {
                ListeningBar(
                    level = vm.micLevel,
                    partial = vm.partialText,
                    onFinish = { vm.finishListening() },
                    onCancel = { vm.cancelListening() }
                )
            } else {
                InputBar(
                    value = input,
                    onChange = { input = it },
                    enabled = !thinking,
                    onMic = onMic,
                    onSend = { submit() }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar la conversación?") },
            text = { Text("Se borran los mensajes, pero NOVA conserva lo que ha aprendido de ti (eso se gestiona en Memoria).") },
            confirmButton = {
                TextButton(onClick = { vm.clearChat(); confirmClear = false }) { Text("Borrar", color = Nova.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
            containerColor = Nova.Surface
        )
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    onMic: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIcon(Icons.Filled.Mic, "Hablar", onClick = onMic, size = 48.dp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF121726))
                .border(1.dp, Nova.Stroke, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (value.isEmpty()) Text("Escribe un mensaje…", color = Nova.Muted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = TextStyle(color = Nova.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Nova.Purple),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(8.dp))
        val canSend = enabled && value.isNotBlank()
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (canSend) Nova.Purple else Color(0xFF2A2F45))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(Icons.Filled.Send, "Enviar", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ListeningBar(level: Float, partial: String, onFinish: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        AnimatedVisibility(partial.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
            Text(
                "“$partial”",
                color = Nova.Text, fontSize = 15.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF121726))
                    .border(1.dp, Color(0x5555C7FF), RoundedCornerShape(26.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Nova.Danger))
                    Spacer(Modifier.width(10.dp))
                    Text("Cancelar", color = Nova.Text, fontSize = 15.sp)
                    Spacer(Modifier.width(12.dp))
                    Waveform(level = level, modifier = Modifier.weight(1f).height(30.dp), bars = 21)
                }
            }
            Spacer(Modifier.width(8.dp))
            RoundIcon(
                Icons.Filled.Send, "Enviar lo dicho", onClick = onFinish, size = 50.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Nova.Blue, Nova.Purple))
            )
        }
    }
}
