package de.msjones.android.alarmapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color.Black,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0000FF),
    onPrimary = Color.Yellow,//(0xFF381E72),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color.Black,//(0xFF1C1B1F),
    onSurface = Color.Yellow,//Black,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun JFAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
        val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = Shapes(),
            content = content
        )
}
