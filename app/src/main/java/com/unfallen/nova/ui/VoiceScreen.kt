package com.unfallen.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.NovaViewModel
import com.unfallen.nova.data.NovaStatus

/** Modo conversación por voz: el alien a pantalla completa y un gran botón para hablar. */
@Composable
fun VoiceScreen(vm: NovaViewModel, onMic: () -> Unit) {
    DisposableEffect(Unit) {
        vm.voiceScreenActive = true
        onDispose { vm.leaveVoiceScreen() }
    }
    val status = vm.status
    val listening = status == NovaStatus.LISTENING
    val lastReply = vm.messages.lastOrNull { it.role == "assistant" }

    Box(Modifier.fillMaxSize().background(Nova.Bg)) {
        AlienAvatar(
            status = status,
            wordTick = vm.wordTick,
            micLevel = vm.micLevel,
            modifier = Modifier.fillMaxSize()
        )

        NovaHeader(status) {
            RoundIcon(
                icon = if (vm.voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                description = "Voz",
                onClick = { vm.updateVoiceEnabled(!vm.voiceEnabled) }
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (listening || status == NovaStatus.SPEAKING) {
                Waveform(
                    level = if (listening) vm.micLevel else 0.6f,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(80.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // Texto: lo que estás diciendo o lo último que ha dicho NOVA
            val caption = when {
                listening -> vm.partialText.ifBlank { "Te estoy escuchando…" }
                status == NovaStatus.THINKING -> "Pensando…"
                lastReply != null -> lastReply.text
                else -> "Toca el botón y háblame. Cuéntame cómo te ha ido el día."
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        caption,
                        color = Nova.Text,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        textAlign = if (listening) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            vm.learnedNotice?.let {
                Spacer(Modifier.height(8.dp))
                NoticeChip(it)
            }
            vm.error?.let {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(it, onClose = { vm.dismissError() })
            }

            Spacer(Modifier.height(18.dp))
            MicOrb(
                listening = listening,
                level = vm.micLevel,
                onClick = {
                    when (status) {
                        NovaStatus.SPEAKING -> vm.stopSpeaking()
                        else -> onMic()
                    }
                },
                size = 124.dp
            )
            Text(
                when (status) {
                    NovaStatus.LISTENING -> "Toca para enviar"
                    NovaStatus.SPEAKING -> "Toca para interrumpir"
                    NovaStatus.THINKING -> "Un momento…"
                    NovaStatus.IDLE -> "Toca para hablar"
                },
                color = Color.White, fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (listening) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF151A28))
                            .border(1.dp, Nova.Stroke, RoundedCornerShape(24.dp))
                            .clickable { vm.cancelListening() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(Nova.Danger))
                            Spacer(Modifier.width(10.dp))
                            Text("Cancelar", color = Nova.Text, fontSize = 15.sp)
                        }
                    }
                } else {
                    Text("Conversación continua", color = Nova.Muted, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = vm.handsFree,
                        onCheckedChange = { vm.updateHandsFree(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Nova.Purple)
                    )
                }
            }
        }
    }
}
