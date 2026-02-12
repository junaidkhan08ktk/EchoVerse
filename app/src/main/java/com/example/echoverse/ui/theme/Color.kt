package com.example.echoverse.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SeedColor = Color(0xFF6C63FF)
val DeepBackground = Color(0xFF03030A)
val SurfaceDark = Color(0xFF0F0F1A)
val SurfaceGlass = Color(0x33101018)
val AccentPrimary = Color(0xFF8C52FF) 
val AccentSecondary = Color(0xFF5CE1E6)
val TextPrimary = Color(0xFFF2F2F2)
val TextSecondary = Color(0xFFAAAAAA)
val ErrorColor = Color(0xFFFF5252)

// Gradients
val PremiumGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF050510),
        Color(0xFF151025)
    )
)

val GlowGradient = Brush.linearGradient(
    colors = listOf(
        AccentPrimary,
        AccentSecondary
    )
)

val GlassBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x20FFFFFF),
        Color(0x05FFFFFF)
    )
)