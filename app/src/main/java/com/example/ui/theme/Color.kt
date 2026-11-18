package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF0B1017)
val ObsidianSurface = Color(0xFF121A24)
val ObsidianSurfaceElevated = Color(0xFF192331)
val ObsidianSurfaceHighest = Color(0xFF223042)
val ObsidianOutline = Color(0xFF2D3E52)

val AmberPrimary = Color(0xFFF59E0B)
val AmberOnPrimary = Color(0xFF1E1300)
val AmberContainer = Color(0xFF422900)
val AmberOnContainer = Color(0xFFFFDDA9)

val CyanSecondary = Color(0xFF38BDF8)
val CyanOnSecondary = Color(0xFF00202E)
val CyanContainer = Color(0xFF0A364B)
val CyanOnContainer = Color(0xFFC5EBFF)

val IndigoTertiary = Color(0xFF818CF8)
val IndigoContainer = Color(0xFF222857)
val IndigoOnContainer = Color(0xFFE0E3FF)

val ValidEmerald = Color(0xFF10B981)
val ErrorRose = Color(0xFFF43F5E)
val ErrorContainerDark = Color(0xFF4C0D1B)
val OnErrorContainerDark = Color(0xFFFFD9DF)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)

// Light mode tokens
val SlateLightBg = Color(0xFFF8FAFC)
val SlateLightSurface = Color(0xFFFFFFFF)
val SlateLightSurfaceVariant = Color(0xFFEFF4F9)
val SlateLightOutline = Color(0xFFCBD5E1)
val AmberLightPrimary = Color(0xFFD97706)
val AmberLightContainer = Color(0xFFFEF3C7)
val AmberLightOnContainer = Color(0xFF451A03)
val CyanLightSecondary = Color(0xFF0284C7)
val CyanLightContainer = Color(0xFFE0F2FE)
val CyanLightOnContainer = Color(0xFF0C4A6E)
val ValidEmeraldDark = Color(0xFF059669)
val ErrorCrimson = Color(0xFFE11D48)

val CoreDarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    tertiary = IndigoTertiary,
    onTertiary = Color(0xFF111538),
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoOnContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF233244),
    error = ErrorRose,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val CoreLightColorScheme = lightColorScheme(
    primary = AmberLightPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberLightContainer,
    onPrimaryContainer = AmberLightOnContainer,
    secondary = CyanLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = CyanLightContainer,
    onSecondaryContainer = CyanLightOnContainer,
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = SlateLightBg,
    onBackground = Color(0xFF0F172A),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = SlateLightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = SlateLightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorCrimson,
    onError = Color.White
)

@Immutable
data class CoreExtendedColors(
    val valid: Color,
    val warning: Color,
    val telemetry: Color,
    val codeSurface: Color,
    val codeBorder: Color
)

val DarkExtendedColors = CoreExtendedColors(
    valid = ValidEmerald,
    warning = AmberPrimary,
    telemetry = CyanSecondary,
    codeSurface = Color(0xFF090E15),
    codeBorder = Color(0xFF233244)
)

val LightExtendedColors = CoreExtendedColors(
    valid = ValidEmeraldDark,
    warning = AmberLightPrimary,
    telemetry = CyanLightSecondary,
    codeSurface = Color(0xFFF1F5F9),
    codeBorder = Color(0xFFCBD5E1)
)

val LocalCoreExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
