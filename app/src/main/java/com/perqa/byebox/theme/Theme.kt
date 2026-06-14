package com.perqa.byebox.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

enum class AppTheme {
  SYSTEM_DYNAMIC,
  MIDNIGHT_AURORA,
  SOLAR_FLARE,
  FOREST_CYBER
}

private val DefaultLightColorScheme = lightColorScheme(
  primary = Purple40,
  secondary = PurpleGrey40,
  tertiary = Pink40
)

private val MidnightAuroraDarkColorScheme = darkColorScheme(
  primary = AuroraPrimaryDark,
  onPrimary = AuroraOnPrimaryDark,
  primaryContainer = AuroraPrimaryContainerDark,
  onPrimaryContainer = AuroraOnPrimaryContainerDark,
  secondary = AuroraSecondaryDark,
  secondaryContainer = AuroraSecondaryContainerDark,
  onSecondaryContainer = AuroraOnSecondaryContainerDark,
  tertiary = AuroraTertiaryDark,
  background = AuroraBackgroundDark,
  surface = AuroraSurfaceDark,
  surfaceVariant = AuroraSurfaceVariantDark,
  onSurfaceVariant = AuroraOnSurfaceVariantDark,
  onBackground = AuroraOnBackgroundDark,
  onSurface = AuroraOnSurfaceDark,
  outlineVariant = AuroraOutlineVariantDark,
  surfaceContainer = Color(0xFF161D2B),
  surfaceContainerLow = Color(0xFF141A27),
  surfaceContainerHigh = Color(0xFF222B3E),
  surfaceContainerHighest = Color(0xFF2C374F),
  surfaceContainerLowest = Color(0xFF0E131E)
)

private val SolarFlareDarkColorScheme = darkColorScheme(
  primary = SolarPrimaryDark,
  onPrimary = SolarOnPrimaryDark,
  primaryContainer = SolarPrimaryContainerDark,
  onPrimaryContainer = SolarOnPrimaryContainerDark,
  secondary = SolarSecondaryDark,
  secondaryContainer = SolarSecondaryContainerDark,
  onSecondaryContainer = SolarOnSecondaryContainerDark,
  tertiary = SolarTertiaryDark,
  background = SolarBackgroundDark,
  surface = SolarSurfaceDark,
  surfaceVariant = SolarSurfaceVariantDark,
  onSurfaceVariant = SolarOnSurfaceVariantDark,
  onBackground = SolarOnBackgroundDark,
  onSurface = SolarOnSurfaceDark,
  outlineVariant = SolarOutlineVariantDark,
  surfaceContainer = Color(0xFF211C19),
  surfaceContainerLow = Color(0xFF1E1A17),
  surfaceContainerHigh = Color(0xFF2E2723),
  surfaceContainerHighest = Color(0xFF38302B),
  surfaceContainerLowest = Color(0xFF14110E)
)

private val ForestCyberDarkColorScheme = darkColorScheme(
  primary = ForestPrimaryDark,
  onPrimary = ForestOnPrimaryDark,
  primaryContainer = ForestPrimaryContainerDark,
  onPrimaryContainer = ForestOnPrimaryContainerDark,
  secondary = ForestSecondaryDark,
  secondaryContainer = ForestSecondaryContainerDark,
  onSecondaryContainer = ForestOnSecondaryContainerDark,
  tertiary = ForestTertiaryDark,
  background = ForestBackgroundDark,
  surface = ForestSurfaceDark,
  surfaceVariant = ForestSurfaceVariantDark,
  onSurfaceVariant = ForestOnSurfaceVariantDark,
  onBackground = ForestOnBackgroundDark,
  onSurface = ForestOnSurfaceDark,
  outlineVariant = ForestOutlineVariantDark,
  surfaceContainer = Color(0xFF192018),
  surfaceContainerLow = Color(0xFF161D15),
  surfaceContainerHigh = Color(0xFF263025),
  surfaceContainerHighest = Color(0xFF2F3C2E),
  surfaceContainerLowest = Color(0xFF0F140F)
)

@Composable
fun ByeBoxTheme(
  appTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme = if (appTheme == AppTheme.SYSTEM_DYNAMIC && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else when (appTheme) {
    AppTheme.SYSTEM_DYNAMIC -> if (darkTheme) MidnightAuroraDarkColorScheme else DefaultLightColorScheme
    AppTheme.MIDNIGHT_AURORA -> MidnightAuroraDarkColorScheme
    AppTheme.SOLAR_FLARE -> SolarFlareDarkColorScheme
    AppTheme.FOREST_CYBER -> ForestCyberDarkColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


