package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AccentPreset(
  val id: String,
  val displayName: String,
  val seedHex: Long
) {
  EMERALD("emerald", "Botanical Emerald", 0xFF0E6B50),
  TERRACOTTA("terracotta", "Sunset Terracotta", 0xFFB84A27),
  COBALT("cobalt", "Electric Cobalt", 0xFF1E56C6),
  PLUM("plum", "Velvet Plum", 0xFF7B337E),
  AMBER("amber", "Golden Amber", 0xFF9C6500),
  SLATE("slate", "Nordic Slate", 0xFF335C67);

  companion object {
    fun fromId(id: String): AccentPreset = entries.find { it.id == id } ?: EMERALD
  }
}

enum class AppThemeMode(val id: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark"),
  HIGH_CONTRAST_LIGHT("high_contrast_light"),
  HIGH_CONTRAST_DARK("high_contrast_dark");

  companion object {
    fun fromId(id: String): AppThemeMode = entries.find { it.id == id } ?: SYSTEM
  }
}

// List / Tag color palette choices for color-blocked cards
val ListPaletteColors = listOf(
  0xFF0E6B50L, // Emerald
  0xFFB84A27L, // Terracotta
  0xFF1E56C6L, // Cobalt
  0xFF7B337EL, // Plum
  0xFF9C6500L, // Amber
  0xFF006874L, // Cyan
  0xFF984061L, // Rose
  0xFF4A672DL  // Moss
)

private val EmeraldLightColors = lightColorScheme(
  primary = Color(0xFF0E6B50),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFA1F2D3),
  onPrimaryContainer = Color(0xFF002116),
  secondary = Color(0xFF4B635A),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFCEE9DD),
  onSecondaryContainer = Color(0xFF072018),
  tertiary = Color(0xFF984816),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFFFDBCA),
  onTertiaryContainer = Color(0xFF331200),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  background = Color(0xFFF5FBF7),
  onBackground = Color(0xFF171D1A),
  surface = Color(0xFFF5FBF7),
  onSurface = Color(0xFF171D1A),
  surfaceVariant = Color(0xFFDBE5DF),
  onSurfaceVariant = Color(0xFF3F4945),
  outline = Color(0xFF6F7975),
  outlineVariant = Color(0xFFBFC9C3),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFEFF5F1),
  surfaceContainer = Color(0xFFE9EFEA),
  surfaceContainerHigh = Color(0xFFE3EAE5),
  surfaceContainerHighest = Color(0xFFDEE4DF)
)

private val EmeraldDarkColors = darkColorScheme(
  primary = Color(0xFF85D6B8),
  onPrimary = Color(0xFF003828),
  primaryContainer = Color(0xFF00513B),
  onPrimaryContainer = Color(0xFFA1F2D3),
  secondary = Color(0xFFB2CCC1),
  onSecondary = Color(0xFF1E352D),
  secondaryContainer = Color(0xFF344C43),
  onSecondaryContainer = Color(0xFFCEE9DD),
  tertiary = Color(0xFFFFB68E),
  onTertiary = Color(0xFF532200),
  tertiaryContainer = Color(0xFF763300),
  onTertiaryContainer = Color(0xFFFFDBCA),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF0F1512),
  onBackground = Color(0xFFDEE4DF),
  surface = Color(0xFF0F1512),
  onSurface = Color(0xFFDEE4DF),
  surfaceVariant = Color(0xFF3F4945),
  onSurfaceVariant = Color(0xFFBFC9C3),
  outline = Color(0xFF89938E),
  outlineVariant = Color(0xFF3F4945),
  surfaceContainerLowest = Color(0xFF0A0F0D),
  surfaceContainerLow = Color(0xFF171D1A),
  surfaceContainer = Color(0xFF1B211E),
  surfaceContainerHigh = Color(0xFF252B28),
  surfaceContainerHighest = Color(0xFF303633)
)

private val HighContrastLightColors = lightColorScheme(
  primary = Color(0xFF003022),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFF00543D),
  onPrimaryContainer = Color(0xFFFFFFFF),
  secondary = Color(0xFF182F27),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFF364E45),
  onSecondaryContainer = Color(0xFFFFFFFF),
  tertiary = Color(0xFF471C00),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFF7A3400),
  onTertiaryContainer = Color(0xFFFFFFFF),
  error = Color(0xFF4E0002),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFF8C0009),
  onErrorContainer = Color(0xFFFFFFFF),
  background = Color(0xFFFFFFFF),
  onBackground = Color(0xFF000000),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF000000),
  surfaceVariant = Color(0xFFDCE5DF),
  onSurfaceVariant = Color(0xFF000000),
  outline = Color(0xFF1F2925),
  outlineVariant = Color(0xFF3F4945)
)

private val HighContrastDarkColors = darkColorScheme(
  primary = Color(0xFFECFFF6),
  onPrimary = Color(0xFF000000),
  primaryContainer = Color(0xFF89DAB9),
  onPrimaryContainer = Color(0xFF000000),
  secondary = Color(0xFFECFFF6),
  onSecondary = Color(0xFF000000),
  secondaryContainer = Color(0xFFB6D0C5),
  onSecondaryContainer = Color(0xFF000000),
  tertiary = Color(0xFFFFFAF8),
  onTertiary = Color(0xFF000000),
  tertiaryContainer = Color(0xFFFFBC98),
  onTertiaryContainer = Color(0xFF000000),
  error = Color(0xFFFFF9F9),
  onError = Color(0xFF000000),
  errorContainer = Color(0xFFFFBAB1),
  onErrorContainer = Color(0xFF000000),
  background = Color(0xFF000000),
  onBackground = Color(0xFFFFFFFF),
  surface = Color(0xFF000000),
  onSurface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFF212B27),
  onSurfaceVariant = Color(0xFFFFFFFF),
  outline = Color(0xFFC3CDC7),
  outlineVariant = Color(0xFFC3CDC7)
)

fun buildPresetColorScheme(
  preset: AccentPreset,
  darkTheme: Boolean,
  highContrast: Boolean = false
): ColorScheme {
  if (highContrast) {
    return if (darkTheme) HighContrastDarkColors else HighContrastLightColors
  }
  val base = if (darkTheme) EmeraldDarkColors else EmeraldLightColors
  return when (preset) {
    AccentPreset.EMERALD -> base
    AccentPreset.TERRACOTTA -> if (darkTheme) {
      base.copy(
        primary = Color(0xFFFFB59A),
        onPrimary = Color(0xFF5B1B00),
        primaryContainer = Color(0xFF802904),
        onPrimaryContainer = Color(0xFFFFDBCF),
        tertiary = Color(0xFF85D6B8)
      )
    } else {
      base.copy(
        primary = Color(0xFFB84A27),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCF),
        onPrimaryContainer = Color(0xFF380D00),
        tertiary = Color(0xFF0E6B50)
      )
    }
    AccentPreset.COBALT -> if (darkTheme) {
      base.copy(
        primary = Color(0xFFB1C5FF),
        onPrimary = Color(0xFF002C71),
        primaryContainer = Color(0xFF00419E),
        onPrimaryContainer = Color(0xFFDAE2FF),
        tertiary = Color(0xFFFFB68E)
      )
    } else {
      base.copy(
        primary = Color(0xFF1E56C6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDAE2FF),
        onPrimaryContainer = Color(0xFF001946),
        tertiary = Color(0xFF984816)
      )
    }
    AccentPreset.PLUM -> if (darkTheme) {
      base.copy(
        primary = Color(0xFFF6AEF9),
        onPrimary = Color(0xFF4F0E55),
        primaryContainer = Color(0xFF68276D),
        onPrimaryContainer = Color(0xFFFFD6FE),
        tertiary = Color(0xFF85D6B8)
      )
    } else {
      base.copy(
        primary = Color(0xFF7B337E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD6FE),
        onPrimaryContainer = Color(0xFF35003B),
        tertiary = Color(0xFF0E6B50)
      )
    }
    AccentPreset.AMBER -> if (darkTheme) {
      base.copy(
        primary = Color(0xFFF9BD48),
        onPrimary = Color(0xFF422C00),
        primaryContainer = Color(0xFF5E4100),
        onPrimaryContainer = Color(0xFFFFDEA6),
        tertiary = Color(0xFF85D6B8)
      )
    } else {
      base.copy(
        primary = Color(0xFF9C6500),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDEA6),
        onPrimaryContainer = Color(0xFF271900),
        tertiary = Color(0xFF0E6B50)
      )
    }
    AccentPreset.SLATE -> if (darkTheme) {
      base.copy(
        primary = Color(0xFF8AD0E3),
        onPrimary = Color(0xFF003540),
        primaryContainer = Color(0xFF004E5C),
        onPrimaryContainer = Color(0xFFB7EAFF),
        tertiary = Color(0xFFFFB68E)
      )
    } else {
      base.copy(
        primary = Color(0xFF335C67),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB7EAFF),
        onPrimaryContainer = Color(0xFF001F26),
        tertiary = Color(0xFF984816)
      )
    }
  }
}
