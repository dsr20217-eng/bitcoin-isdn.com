package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

val CoreLensShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object CoreLensTheme {
    val extendedColors: CoreExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCoreExtendedColors.current
}

@Composable
fun CoreLensTheme(
    darkTheme: Boolean = true,
    followSystem: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = if (followSystem) isSystemInDarkTheme() else darkTheme
    val colorScheme = if (useDark) CoreDarkColorScheme else CoreLightColorScheme
    val extended = if (useDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalCoreExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CoreLensTypography,
            shapes = CoreLensShapes,
            content = content
        )
    }
}
