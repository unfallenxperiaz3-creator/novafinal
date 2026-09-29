package com.unfallen.nova.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfallen.nova.data.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ---------------------------------------------------------------- Onda de audio

/** Onda simétrica tipo "voz" como en el diseño. level 0..1 */
@Composable
fun Waveform(level: Float, modifier: Modifier = Modifier, bars: Int = 31, active: Boolean = true) {
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val lvl by animateFloatAsState(if (active) level.coerceIn(0f, 1f) else 0f, tween(120), label = "lvl")
    Canvas(modifier) {
        val gap = size.width / (bars * 1.8f)
        val barW = gap * 0.8f
        val mid = bars / 2f
        for (i in 0 until bars) {
            val dist = abs(i - mid) / mid            // 0 centro, 1 bordes
            val env = 1f - dist * 0.85f
            val wobble = 0.5f + 0.5f * sin(phase * 2f + i * 0.7f)
            val h = size.height * (0.08f + env * (0.18f + lvl * 0.74f) * (0.45f + 0.55f * wobble))
            val x = i * (size.width / bars) + (size.width / bars - barW) / 2f
            drawRoundRect(
                brush = Nova.waveBrush,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                alpha = 0.55f + 0.45f * env
            )
        }
    }
}

// ---------------------------------------------------------------- Botón micro grande

@Composable
fun MicOrb(
    listening: Boolean,
    level: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp
) {
    val inf = rememberInfiniteTransition(label = "orb")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "pulse"
    )
    val lvl by animateFloatAsState(if (listening) level else 0f, tween(100), label = "orbLvl")

    Box(
        modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                // anillos de pulso
                val pulses = if (listening) 2 else 1
                for (k in 0 until pulses) {
                    val p = (pulse + k * 0.5f) % 1f
                    drawCircle(
                        color = (if (listening) Nova.Cyan else Nova.Violet).copy(alpha = (1f - p) * 0.35f),
                        radius = r * (0.9f + p * 0.35f + lvl * 0.15f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                // halo
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf((if (listening) Nova.Blue else Nova.Purple).copy(alpha = 0.45f + lvl * 0.3f), Color.Transparent),
                        center = center, radius = r * 1.25f
                    ),
                    radius = r * 1.25f
                )
                // fondo oscuro
                drawCircle(color = Color(0xE60A0D18), radius = r * 0.86f)
                // aro de degradado girando
                rotate(rot) {
                    drawCircle(brush = Nova.orbBrush, radius = r * 0.86f, style = Stroke(width = 4.dp.toPx()))
                }
            }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (listening) Icons.Filled.Mic else Icons.Filled.GraphicEq,
            contentDescription = if (listening) "Terminar de hablar" else "Toca para hablar",
            tint = Color.White,
            modifier = Modifier.size(size * 0.36f)
        )
    }
}

// ---------------------------------------------------------------- Botones pequeños

@Composable
fun RoundIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color(0x991A2033),
    tint: Color = Color.White,
    brush: Brush? = null
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(background))
            .border(1.dp, Nova.Stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xB3121726))
            .border(1.dp, Nova.Stroke, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) { content() }
}

// ---------------------------------------------------------------- Burbujas

private val hourFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun ChatBubble(
    msg: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit
) {
    val isUser = msg.role == "user"
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 6.dp,
            bottomEnd = if (isUser) 6.dp else 20.dp
        )
        Column(
            Modifier
                .widthIn(max = 310.dp)
                .clip(shape)
                .then(
                    if (isUser) Modifier.background(Nova.userBubble)
                    else Modifier
                        .background(Color(0xE6141A29))
                        .border(1.dp, if (isSpeaking) Nova.Purple else Nova.Stroke, shape)
                )
                .padding(start = 15.dp, end = 15.dp, top = 11.dp, bottom = 7.dp)
        ) {
            SelectionContainer {
                Text(
                    text = if (isUser) AnnotatedString(msg.text) else richText(msg.text),
                    color = Nova.Text,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
            Row(
                Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.viaVoice) {
                    Icon(Icons.Filled.Mic, null, tint = Color(0xB3FFFFFF), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(hourFmt.format(Date(msg.time)), color = Color(0xB3FFFFFF), fontSize = 11.sp)
                if (isUser) {
                    Spacer(Modifier.width(4.dp))
                    Text("✓✓", color = Color(0xCCBFD4FF), fontSize = 11.sp)
                } else {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) Nova.Purple else Color(0x332E3650))
                            .clickable { if (isSpeaking) onStop() else onSpeak() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Parar voz" else "Escuchar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Markdown mínimo: **negrita**, títulos con # y viñetas. */
fun richText(src: String): AnnotatedString = buildAnnotatedString {
    val lines = src.lines()
    lines.forEachIndexed { idx, rawLine ->
        var line = rawLine
        var heading = false
        val h = Regex("^\\s{0,3}#{1,6}\\s+").find(line)
        if (h != null) {
            line = line.substring(h.range.last + 1); heading = true
        }
        val b = Regex("^\\s*[-*]\\s+").find(line)
        if (b != null) line = "•  " + line.substring(b.range.last + 1)

        val start = length
        var i = 0
        var bold = false
        while (i < line.length) {
            if (line.startsWith("**", i)) {
                bold = !bold; i += 2; continue
            }
            val c = line[i]
            if (bold) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(c); pop()
            } else append(c)
            i++
        }
        if (heading) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
        if (idx < lines.lastIndex) append('\n')
    }
}

// ---------------------------------------------------------------- Avisos

@Composable
fun ErrorBanner(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE63A1520))
            .border(1.dp, Color(0x55FF5A6A), RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color(0xFFFFD6DB), fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, "Cerrar", tint = Color(0xFFFFD6DB), modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun NoticeChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Nova.Text,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xE61E1740))
            .border(1.dp, Color(0x667B5CFF), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")
    val t by inf.animateFloat(0f, 3f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "t")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val active = t.toInt() == i
            Box(
                Modifier
                    .size(if (active) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (active) Nova.Violet else Color(0x667B5CFF))
            )
        }
    }
}
