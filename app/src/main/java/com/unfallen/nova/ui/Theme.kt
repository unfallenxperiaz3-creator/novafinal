package com.unfallen.nova.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Nova {
    val Bg = Color(0xFF06080E)
    val Bg2 = Color(0xFF0B0F1A)
    val Surface = Color(0xFF141A29)
    val SurfaceHi = Color(0xFF1C2336)
    val Stroke = Color(0x22FFFFFF)
    val Purple = Color(0xFF7B5CFF)
    val PurpleDeep = Color(0xFF5B3DF5)
    val Violet = Color(0xFFB45CFF)
    val Blue = Color(0xFF4F8CFF)
    val Cyan = Color(0xFF55C7FF)
    val Text = Color(0xFFF2F4FA)
    val Muted = Color(0xFF9AA3B8)
    val Danger = Color(0xFFFF5A6A)
    val Success = Color(0xFF3DDC97)

    val orbBrush = Brush.sweepGradient(listOf(Violet, Blue, Cyan, Purple, Violet))
    val userBubble = Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF8A5CFF)))
    val waveBrush = Brush.horizontalGradient(listOf(Violet, Blue, Cyan, Blue, Violet))
}

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Nova.Purple,
            onPrimary = Color.White,
            secondary = Nova.Cyan,
            background = Nova.Bg,
            onBackground = Nova.Text,
            surface = Nova.Surface,
            onSurface = Nova.Text,
            surfaceVariant = Nova.SurfaceHi,
            onSurfaceVariant = Nova.Muted,
            error = Nova.Danger
        ),
        content = content
    )
}
