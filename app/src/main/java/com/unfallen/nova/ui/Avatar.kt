package com.unfallen.nova.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.unfallen.nova.R
import com.unfallen.nova.data.NovaStatus
import kotlin.math.max

/**
 * El alien de NOVA a pantalla completa dentro de su caja.
 * - Reposo: respira y flota suavemente.
 * - Escuchando: se acerca un poco y el brillo azul sigue el volumen de tu voz.
 * - Pensando: inclina la cabeza despacio, brillo violeta pulsante.
 * - Hablando: la cabeza "asiente" con cada palabra que pronuncia la voz.
 */
@Composable
fun AlienAvatar(
    status: NovaStatus,
    wordTick: Int,
    micLevel: Float,
    modifier: Modifier = Modifier,
    fadeBottomTo: Color = Nova.Bg
) {
    val speaking = status == NovaStatus.SPEAKING
    val thinking = status == NovaStatus.THINKING
    val listening = status == NovaStatus.LISTENING

    val inf = rememberInfiniteTransition(label = "alien")
    val breath by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val sway by inf.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
    val thinkPulse by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "thinkPulse"
    )
    val chatter by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(170, easing = LinearEasing), RepeatMode.Reverse),
        label = "chatter"
    )

    // Golpe de "habla" en cada palabra
    val talk = remember { Animatable(0f) }
    LaunchedEffect(wordTick) {
        if (speaking) {
            talk.animateTo(1f, tween(80))
            talk.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 320f))
        }
    }
    LaunchedEffect(speaking) {
        if (!speaking) talk.animateTo(0f, tween(200))
    }
    // Si el motor de voz no avisa por palabra, usamos una oscilación de respaldo
    val talkAmount = if (speaking) max(talk.value, chatter * 0.35f) else talk.value

    val smoothMic by animateFloatAsState(if (listening) micLevel else 0f, tween(120), label = "mic")
    val lean by animateFloatAsState(if (listening) 1f else 0f, tween(500), label = "lean")
    val tiltAmp by animateFloatAsState(
        when {
            thinking -> 3.2f
            speaking -> 1.4f
            else -> 0.7f
        }, tween(700), label = "tilt"
    )

    val glowColor by animateColorAsState(
        when (status) {
            NovaStatus.LISTENING -> Nova.Cyan
            NovaStatus.THINKING -> Nova.Violet
            NovaStatus.SPEAKING -> Nova.Purple
            NovaStatus.IDLE -> Nova.PurpleDeep
        }, tween(600), label = "glow"
    )
    val glowAlpha = when (status) {
        NovaStatus.IDLE -> 0.18f + breath * 0.10f
        NovaStatus.LISTENING -> 0.30f + smoothMic * 0.55f
        NovaStatus.THINKING -> 0.22f + thinkPulse * 0.30f
        NovaStatus.SPEAKING -> 0.30f + talkAmount * 0.45f
    }

    Box(modifier.clipToBounds()) {
        Image(
            painter = painterResource(R.drawable.nova_avatar),
            contentDescription = "NOVA",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0.04f, -0.3f),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val base = 1.10f + breath * 0.015f + lean * 0.035f + smoothMic * 0.02f
                    scaleX = base + talkAmount * 0.008f
                    scaleY = base + talkAmount * 0.022f
                    rotationZ = sway * tiltAmp
                    // GraphicsLayerScope ya es un Density: .dp.toPx() funciona aquí
                    translationY = ((breath - 0.5f) * 7f).dp.toPx() -
                        (talkAmount * 5f).dp.toPx() +
                        (lean * 6f).dp.toPx()
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                }
        )

        Canvas(Modifier.fillMaxSize()) {
            // Brillo del pecho / aura según el estado
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.92f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width / 2f, size.height * 0.92f),
                blendMode = BlendMode.Screen
            )
            // Oscurecer arriba (para el título) y fundir abajo con el fondo
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.22f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to fadeBottomTo
                )
            )
        }
    }
}

