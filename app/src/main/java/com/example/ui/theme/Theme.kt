package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameColorScheme = darkColorScheme(
  primary = GoldAccent,
  onPrimary = Color.Black,
  primaryContainer = TableFeltMid,
  onPrimaryContainer = TextLight,
  secondary = UnoBlue,
  onSecondary = Color.White,
  tertiary = UnoRed,
  onTertiary = Color.White,
  background = TableFeltDark,
  onBackground = TextLight,
  surface = CardSurfaceDark,
  onSurface = TextLight,
  surfaceVariant = TableFeltLight,
  onSurfaceVariant = TextMuted
)

@Composable
fun ColorClashTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = GameColorScheme,
    typography = GameTypography,
    content = content
  )
}
