package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NothingDarkColorScheme =
  darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    primaryContainer = NothingSurfaceHighlight,
    onPrimaryContainer = NothingWhite,
    secondary = NothingLightGray,
    onSecondary = NothingBlack,
    secondaryContainer = NothingSurfaceVariant,
    onSecondaryContainer = NothingWhite,
    tertiary = NothingRed,
    onTertiary = NothingWhite,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingDark,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurface,
    onSurfaceVariant = NothingGray,
    outline = NothingBorder,
    outlineVariant = NothingDarkGray,
  )

private val NothingLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF007AFF), // iOS System Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF007AFF),
    secondary = Color(0xFF5856D6), // iOS Purple
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2F2F7),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFFFF2D55), // iOS Pink/Red
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF2F2F7), // iOS System Grouped Background
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF), // iOS Pure White Card Surface
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF8E8E93), // iOS Secondary Label
    outline = Color(0xFFE5E5EA), // iOS Hairline Separator
    outlineVariant = Color(0xFFD1D1D6),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to dark Nothing aesthetic
  dynamicColor: Boolean = false, // Keep intentional Nothing monochrome aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) NothingDarkColorScheme else NothingLightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

