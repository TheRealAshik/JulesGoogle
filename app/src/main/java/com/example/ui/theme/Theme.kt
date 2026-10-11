package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ChatgptWhite,
    onPrimary = ChatgptBlack,
    secondary = ChatgptBlueGlow,
    onSecondary = ChatgptWhite,
    tertiary = ChatgptGreen,
    onTertiary = ChatgptWhite,
    background = ChatgptDarkBg,
    onBackground = ChatgptDarkText,
    surface = ChatgptDarkSurface,
    onSurface = ChatgptDarkText,
    surfaceVariant = ChatgptDarkCard,
    onSurfaceVariant = ChatgptDarkMuted,
    outline = ChatgptDarkBorder,
    outlineVariant = ChatgptDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ChatgptBlack,
    onPrimary = ChatgptWhite,
    secondary = ChatgptBlue,
    onSecondary = ChatgptWhite,
    tertiary = ChatgptGreen,
    onTertiary = ChatgptWhite,
    background = ChatgptWhite,
    onBackground = ChatgptLightText,
    surface = ChatgptLightSurface,
    onSurface = ChatgptLightText,
    surfaceVariant = ChatgptLightCard,
    onSurfaceVariant = ChatgptLightMuted,
    outline = ChatgptLightBorder,
    outlineVariant = ChatgptLightBorder
)

@Composable
fun JulesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
