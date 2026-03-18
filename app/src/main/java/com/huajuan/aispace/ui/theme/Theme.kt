package com.huajuan.aispace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun HuaJuanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = BrandPrimary,
            onPrimary = Color.White,
            primaryContainer = BrandPrimaryDark,
            onPrimaryContainer = Color.White,
            secondary = BrandSecondary,
            onSecondary = Color.White,
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = DarkOnSurface,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurface = DarkOnSurface,
            onSurfaceVariant = DarkOnSurfaceVariant,
            background = DarkSurface,
            onBackground = DarkOnSurface,
            outline = DarkOutline
        )
        else -> lightColorScheme(
            primary = BrandPrimary,
            onPrimary = Color.White,
            primaryContainer = BrandSurfaceVariant,
            onPrimaryContainer = BrandOnSurface,
            secondary = BrandSecondary,
            onSecondary = Color.White,
            secondaryContainer = BrandSurfaceVariant,
            onSecondaryContainer = BrandOnSurface,
            surface = BrandSurface,
            surfaceVariant = BrandSurfaceVariant,
            onSurface = BrandOnSurface,
            onSurfaceVariant = BrandOnSurfaceVariant,
            background = BrandSurface,
            onBackground = BrandOnSurface,
            outline = BrandOutline
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HuaJuanShapes,
        content = content
    )
}
