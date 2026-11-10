package io.github.flassuu.talex.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F54),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F2CF),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4D6357),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD6B4),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFFA8F2CF),
    secondary = Color(0xFFB4CCBC),
    background = Color(0xFF191C1A),
    surface = Color(0xFF191C1A),
)

/**
 * Material 3 theme with dynamic color where the platform supports it (D-040).
 */
@Composable
fun TalexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
