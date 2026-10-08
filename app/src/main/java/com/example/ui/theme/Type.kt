package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val FredokaFontFamily = FontFamily(
  Font(R.font.fredoka, FontWeight.Normal)
)

val GameTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = 0.5.sp
  ),
  displayMedium = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 36.sp
  ),
  titleLarge = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 18.sp,
    lineHeight = 22.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 20.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  labelLarge = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp
  )
)
