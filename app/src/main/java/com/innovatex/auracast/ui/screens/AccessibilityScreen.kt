package com.innovatex.auracast.ui.screens

import com.innovatex.auracast.R
import com.innovatex.auracast.ui.i18n.appString
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.ui.components.ConnectionFeedbackHost
import com.innovatex.auracast.ui.theme.AppLanguage
import com.innovatex.auracast.ui.theme.AppearanceMode
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings
import com.innovatex.auracast.ui.theme.LocalUpdateAccessibilitySettings
import com.innovatex.auracast.ui.theme.TextSizeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val settings = LocalAccessibilitySettings.current
    val updateSettings = LocalUpdateAccessibilitySettings.current
    var previewCount by remember { mutableIntStateOf(0) }

    val previewEvent = if (previewCount > 0) {
        "accessibility-preview-$previewCount"
    } else {
        null
    }

    ConnectionFeedbackHost(eventKey = previewEvent, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = appString(R.string.accessibility),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = appString(R.string.settings_intro),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()
                Text(appString(R.string.language), style = MaterialTheme.typography.titleMedium)
                Text(
                    appString(R.string.language_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.language == option,
                                    onClick = {
                                        updateSettings(settings.copy(language = option))
                                    },
                                    role = Role.RadioButton
                                )
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.language == option,
                                onClick = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (option == AppLanguage.CHINESE_SIMPLIFIED) {
                                    appString(R.string.language_chinese)
                                } else {
                                    appString(R.string.language_english)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text(appString(R.string.theme), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.selectableGroup()) {
                    AppearanceMode.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.appearance == option,
                                    onClick = {
                                        updateSettings(settings.copy(appearance = option))
                                    },
                                    role = Role.RadioButton
                                )
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.appearance == option,
                                onClick = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (option == AppearanceMode.COLOURFUL) {
                                    appString(R.string.colourful)
                                } else {
                                    appString(R.string.black_white)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                val themeSwatches = if (settings.appearance == AppearanceMode.COLOURFUL) {
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        Color(0xFFF4B641) // Warm yellow accent
                    )
                } else {
                    listOf(Color.Black, Color(0xFF666666), Color(0xFFCACACA), Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    themeSwatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(color = swatch, shape = CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                    Text(
                        appString(R.string.theme_preview),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()
                Text(appString(R.string.text_size), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TextSizeOption.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.textSize == option,
                            onClick = {
                                updateSettings(settings.copy(textSize = option))
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TextSizeOption.entries.size
                            )
                        ) {
                            Text(when (option) {
                                TextSizeOption.STANDARD -> appString(R.string.text_standard)
                                TextSizeOption.LARGE -> appString(R.string.text_large)
                                TextSizeOption.LARGEST -> appString(R.string.text_largest)
                            })
                        }
                    }
                }
                Text(
                    appString(R.string.preview_next_stop),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()
                SettingToggleRow(
                    title = appString(R.string.vibrate_on_connect),
                    detail = appString(R.string.vibrate_detail),
                    checked = settings.vibrateOnConnect,
                    onCheckedChange = { enabled ->
                        updateSettings(settings.copy(vibrateOnConnect = enabled))
                    }
                )

                HorizontalDivider()
                SettingToggleRow(
                    title = appString(R.string.flash_on_connect),
                    detail = appString(R.string.flash_detail),
                    checked = settings.flashOnConnect,
                    onCheckedChange = { enabled ->
                        updateSettings(settings.copy(flashOnConnect = enabled))
                    }
                )

                if (settings.vibrateOnConnect || settings.flashOnConnect) {
                    OutlinedButton(
                        onClick = { previewCount += 1 },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(appString(R.string.test_connection_feedback))
                    }
                }

                HorizontalDivider()
                SettingToggleRow(
                    title = appString(R.string.keep_screen_on),
                    detail = appString(R.string.keep_screen_on_detail),
                    checked = settings.keepScreenOn,
                    onCheckedChange = { enabled ->
                        updateSettings(settings.copy(keepScreenOn = enabled))
                    }
                )
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(56.dp)
            ) {
                Text(appString(R.string.done))
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}
