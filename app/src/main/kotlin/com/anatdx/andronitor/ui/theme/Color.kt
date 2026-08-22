package com.anatdx.andronitor.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback palette used when dynamic color is off, or on API < 31 where the
// wallpaper-derived scheme is unavailable. Seeded from X's brand blue so it sits
// next to the verified badge (see ProfileScreen's VerifiedBlue) without clashing.

private val Blue10 = Color(0xFF001E2F)
private val Blue20 = Color(0xFF00344F)
private val Blue30 = Color(0xFF004A6E)
private val Blue40 = Color(0xFF00628F)
private val Blue80 = Color(0xFF8ACDFF)
private val Blue90 = Color(0xFFC9E6FF)

private val Slate10 = Color(0xFF0D1D2A)
private val Slate20 = Color(0xFF233240)
private val Slate30 = Color(0xFF3A4857)
private val Slate40 = Color(0xFF52606F)
private val Slate80 = Color(0xFFB9C8D9)
private val Slate90 = Color(0xFFD5E4F6)

private val Violet10 = Color(0xFF1E1732)
private val Violet20 = Color(0xFF332C48)
private val Violet30 = Color(0xFF4A4260)
private val Violet40 = Color(0xFF625979)
private val Violet80 = Color(0xFFCCC0E7)
private val Violet90 = Color(0xFFE9DDFF)

private val Neutral4 = Color(0xFF0A0E12)
private val Neutral6 = Color(0xFF0F1317)
private val Neutral10 = Color(0xFF191C20)
private val Neutral12 = Color(0xFF1D2024)
private val Neutral17 = Color(0xFF272A2E)
private val Neutral20 = Color(0xFF2E3135)
private val Neutral22 = Color(0xFF32353A)
private val Neutral24 = Color(0xFF36393E)
private val Neutral87 = Color(0xFFDAD9DD)
private val Neutral90 = Color(0xFFE2E2E5)
private val Neutral92 = Color(0xFFE8E8EB)
private val Neutral94 = Color(0xFFEEEDF1)
private val Neutral95 = Color(0xFFF1F0F4)
private val Neutral96 = Color(0xFFF4F3F7)
private val Neutral98 = Color(0xFFFAF9FD)

private val NeutralVariant30 = Color(0xFF41484D)
private val NeutralVariant50 = Color(0xFF71787E)
private val NeutralVariant60 = Color(0xFF8B9198)
private val NeutralVariant80 = Color(0xFFC1C7CE)
private val NeutralVariant90 = Color(0xFFDDE3EA)

internal val FallbackLightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    inversePrimary = Blue80,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Violet40,
    onTertiary = Color.White,
    tertiaryContainer = Violet90,
    onTertiaryContainer = Violet10,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    surfaceDim = Neutral87,
    surfaceBright = Neutral98,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

internal val FallbackDarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    inversePrimary = Blue40,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Violet80,
    onTertiary = Violet20,
    tertiaryContainer = Violet30,
    onTertiaryContainer = Violet90,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color.Black,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    surfaceDim = Neutral6,
    surfaceBright = Neutral24,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)
