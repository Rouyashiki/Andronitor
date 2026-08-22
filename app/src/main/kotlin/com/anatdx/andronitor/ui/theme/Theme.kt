package com.anatdx.andronitor.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.anatdx.andronitor.model.ThemeMode
import com.anatdx.andronitor.ui.component.clickHapticFeedback

@Composable
fun AndronitorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }

    SystemBarStyleEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        // Drives every built-in transition, so overriding it here is what makes
        // the whole app animate the expressive way rather than the standard way.
        motionScheme = MotionScheme.expressive(),
    ) {
        Box(Modifier.clickHapticFeedback()) { content() }
    }
}

/**
 * Keeps the system bar icons readable when the app theme disagrees with the
 * system one — forcing light mode on a dark-mode device has to flip them back.
 */
@Composable
private fun SystemBarStyleEffect(darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    SideEffect {
        val transparent = Color.Transparent.toArgb()
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(transparent)
            } else {
                SystemBarStyle.light(transparent, transparent)
            },
        )
    }
}
