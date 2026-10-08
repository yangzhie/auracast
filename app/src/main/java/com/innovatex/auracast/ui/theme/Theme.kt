package com.innovatex.auracast.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Both themes are light; Black & white is the initial selection. */
enum class AppearanceMode(val label: String) {
    BLACK_AND_WHITE("Black & white"),
    COLOURFUL("Colourful")
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    CHINESE_SIMPLIFIED("zh-CN")
}

enum class TextSizeOption(val label: String, val multiplier: Float) {
    STANDARD("Standard", 1.00f),
    LARGE("Large", 1.15f),
    LARGEST("Largest", 1.30f)
}

data class AccessibilitySettings(
    val appearance: AppearanceMode = AppearanceMode.BLACK_AND_WHITE,
    val textSize: TextSizeOption = TextSizeOption.STANDARD,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val vibrateOnConnect: Boolean = true,
    val flashOnConnect: Boolean = false,
    val keepScreenOn: Boolean = true
)

val LocalAccessibilitySettings = compositionLocalOf { AccessibilitySettings() }
val LocalUpdateAccessibilitySettings =
    compositionLocalOf<(AccessibilitySettings) -> Unit> { {} }

private const val PREFS_NAME = "auracast_accessibility"
private const val KEY_APPEARANCE = "appearance"
private const val KEY_TEXT_SIZE = "text_size"
private const val KEY_LANGUAGE = "language"
private const val KEY_VIBRATE = "vibrate_on_connect"
private const val KEY_FLASH = "flash_on_connect"
private const val KEY_KEEP_ON = "keep_screen_on"

private fun readSettings(prefs: SharedPreferences): AccessibilitySettings {
    val appearance = if (prefs.getString(KEY_APPEARANCE, "") == AppearanceMode.COLOURFUL.name) {
        AppearanceMode.COLOURFUL
    } else {
        AppearanceMode.BLACK_AND_WHITE
    }

    val textSize = when (prefs.getString(KEY_TEXT_SIZE, TextSizeOption.STANDARD.name)) {
        TextSizeOption.LARGE.name -> TextSizeOption.LARGE
        TextSizeOption.LARGEST.name -> TextSizeOption.LARGEST
        else -> TextSizeOption.STANDARD
    }

    return AccessibilitySettings(
        appearance = appearance,
        textSize = textSize,
        language = if (prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) == AppLanguage.CHINESE_SIMPLIFIED.code) {
            AppLanguage.CHINESE_SIMPLIFIED
        } else {
            AppLanguage.ENGLISH
        },
        vibrateOnConnect = prefs.getBoolean(KEY_VIBRATE, true),
        flashOnConnect = prefs.getBoolean(KEY_FLASH, false),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_ON, true)
    )
}

private fun saveSettings(prefs: SharedPreferences, settings: AccessibilitySettings) {
    prefs.edit()
        .putString(KEY_APPEARANCE, settings.appearance.name)
        .putString(KEY_TEXT_SIZE, settings.textSize.name)
        .putString(KEY_LANGUAGE, settings.language.code)
        .putBoolean(KEY_VIBRATE, settings.vibrateOnConnect)
        .putBoolean(KEY_FLASH, settings.flashOnConnect)
        .putBoolean(KEY_KEEP_ON, settings.keepScreenOn)
        .apply()
}

private val MonochromeColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF303030),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F1F1),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF444444),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF6F6F6),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFFD3D3D3),
    error = Color.Black,
    onError = Color.White
)

// Colourful mode: indigo, teal, berry and warm gold accents.
// Dark text on pale surfaces preserves readability at larger text sizes.
private val ColourfulColors = lightColorScheme(
    primary = Color(0xFF4436C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E6FF),
    onPrimaryContainer = Color(0xFF23165F),
    secondary = Color(0xFF006F69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F5EC),
    onSecondaryContainer = Color(0xFF064A42),
    tertiary = Color(0xFFA62B72),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE1EF),
    onTertiaryContainer = Color(0xFF611343),
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1D1C32),
    surface = Color.White,
    onSurface = Color(0xFF1D1C32),
    surfaceVariant = Color(0xFFEEF1FF),
    onSurfaceVariant = Color(0xFF424457),
    outline = Color(0xFF666779),
    outlineVariant = Color(0xFFD3D1E6),
    error = Color(0xFFB42331),
    onError = Color.White
)

@Composable
fun AuracastTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var settings by remember(preferences) {
        mutableStateOf(readSettings(preferences))
    }

    val updateSettings: (AccessibilitySettings) -> Unit = { changed ->
        settings = changed
        saveSettings(preferences, changed)
    }

    val palette = if (settings.appearance == AppearanceMode.COLOURFUL) {
        ColourfulColors
    } else {
        MonochromeColors
    }

    // Composition-local density scales ALL Compose 'sp' text, including custom
    // HomeScreen text sizes, rather than just MaterialTheme typography styles.
    val systemDensity = LocalDensity.current
    val effectiveDensity = remember(systemDensity, settings.textSize) {
        Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale * settings.textSize.multiplier
        )
    }

    CompositionLocalProvider(
        LocalAccessibilitySettings provides settings,
        LocalUpdateAccessibilitySettings provides updateSettings,
        LocalDensity provides effectiveDensity
    ) {
        MaterialTheme(
            colorScheme = palette,
            typography = Typography,
            content = content
        )
    }
}
