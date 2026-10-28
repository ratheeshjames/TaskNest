package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(
  Font(R.font.space_grotesk, FontWeight.Normal),
  Font(R.font.space_grotesk, FontWeight.Medium),
  Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium)
)

/**
 * Builds the Material 3 Expressive emphasized typography hierarchy, scaled by [fontScaleMultiplier]
 * for user-adjustable font sizing in Settings.
 */
fun buildExpressiveTypography(fontScaleMultiplier: Float = 1.0f): Typography {
  val s = fontScaleMultiplier.coerceIn(0.85f, 1.35f)
  return Typography(
    displayLarge = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (57f * s).sp,
      lineHeight = (64f * s).sp,
      letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (45f * s).sp,
      lineHeight = (52f * s).sp,
      letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (36f * s).sp,
      lineHeight = (44f * s).sp,
      letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (32f * s).sp,
      lineHeight = (40f * s).sp,
      letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (28f * s).sp,
      lineHeight = (36f * s).sp,
      letterSpacing = (-0.15).sp
    ),
    headlineSmall = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (24f * s).sp,
      lineHeight = (32f * s).sp,
      letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (22f * s).sp,
      lineHeight = (28f * s).sp,
      letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (16f * s).sp,
      lineHeight = (24f * s).sp,
      letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (14f * s).sp,
      lineHeight = (20f * s).sp,
      letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (16f * s).sp,
      lineHeight = (24f * s).sp,
      letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (14f * s).sp,
      lineHeight = (20f * s).sp,
      letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (12f * s).sp,
      lineHeight = (16f * s).sp,
      letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (14f * s).sp,
      lineHeight = (20f * s).sp,
      letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (12f * s).sp,
      lineHeight = (16f * s).sp,
      letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = (11f * s).sp,
      lineHeight = (16f * s).sp,
      letterSpacing = 0.4.sp
    )
  )
}
