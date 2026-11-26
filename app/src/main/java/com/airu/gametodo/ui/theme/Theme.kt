package com.airu.gametodo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val OnAccent = Color(0xFF04181C)

private val DarkScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = OnAccent,
    primaryContainer = AquaDeep,
    onPrimaryContainer = Aqua,
    secondary = Violet,
    onSecondary = OnAccent,
    tertiary = Mint,
    onTertiary = OnAccent,
    background = Abyss,
    onBackground = InkText,
    surface = SurfaceLow,
    onSurface = InkText,
    surfaceVariant = SurfaceMid,
    onSurfaceVariant = InkMuted,
    surfaceContainer = SurfaceMid,
    surfaceContainerHigh = SurfaceHigh,
    outline = HairLine,
    outlineVariant = HairLine,
    error = Coral,
    onError = OnAccent
)

private val LightScheme = lightColorScheme(
    primary = TealDeep,
    onPrimary = PaperSurface,
    primaryContainer = Color(0xFFCFEEF2),
    onPrimaryContainer = Color(0xFF00363D),
    secondary = Color(0xFF5B4BC4),
    onSecondary = PaperSurface,
    tertiary = Color(0xFF1F7A5C),
    onTertiary = PaperSurface,
    background = PaperBg,
    onBackground = InkDark,
    surface = PaperSurface,
    onSurface = InkDark,
    surfaceVariant = PaperSurfaceMid,
    onSurfaceVariant = InkGray,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceMid,
    outline = Color(0xFFCBD5DF),
    outlineVariant = Color(0xFFDFE6ED),
    error = Color(0xFFC2384B),
    onError = PaperSurface
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun GameTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
