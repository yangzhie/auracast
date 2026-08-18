package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.ui.theme.Muted

enum class TextSizeOption(val label: String) {
    STANDARD("Standard"),
    LARGE("Large"),
    LARGEST("Largest")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(modifier: Modifier = Modifier) {
    var textSize by remember { mutableStateOf(TextSizeOption.LARGE) }
    var vibrateOnConnect by remember { mutableStateOf(true) }
    var flashOnConnect by remember { mutableStateOf(false) }
    var extraContrast by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Text size",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TextSizeOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = textSize == option,
                        onClick = { textSize = option },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TextSizeOption.entries.size
                        )
                    ) {
                        Text(option.label)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SettingToggleRow(
                title = "Vibrate on connect",
                detail = "Feel a pulse when a stop's announcements start",
                checked = vibrateOnConnect,
                onCheckedChange = { vibrateOnConnect = it }
            )
            HorizontalDivider()
            SettingToggleRow(
                title = "Flash screen on connect",
                detail = "Brief full-screen flash",
                checked = flashOnConnect,
                onCheckedChange = { flashOnConnect = it }
            )
            HorizontalDivider()
            SettingToggleRow(
                title = "Extra contrast",
                detail = "Stronger borders and darker text",
                checked = extraContrast,
                onCheckedChange = { extraContrast = it }
            )
            HorizontalDivider()
            SettingToggleRow(
                title = "Keep screen on",
                detail = "While a journey is running",
                checked = keepScreenOn,
                onCheckedChange = { keepScreenOn = it }
            )
        }

        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}