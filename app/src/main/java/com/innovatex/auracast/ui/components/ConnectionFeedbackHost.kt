package com.innovatex.auracast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings
import kotlinx.coroutines.delay


@Composable
fun ConnectionFeedbackHost(
    eventKey: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val settings = LocalAccessibilitySettings.current
    val haptics = LocalHapticFeedback.current
    var flashing by rememberSaveable { mutableStateOf(false) }
    var lastEventKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(eventKey) {
        flashing = false
        if (eventKey != null && eventKey != lastEventKey) {
            lastEventKey = eventKey

            if (settings.vibrateOnConnect) {
                // Uses platform haptic feedback; respects the user's device settings.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            if (settings.flashOnConnect) {
                // ONE brief flash per event, not a blinking/strobe animation.
                flashing = true
                delay(180L)
                flashing = false
            }
        }
    }

    Box(modifier = modifier) {
        content()
        if (flashing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White)
                    .semantics { invisibleToUser() }
            )
        }
    }
}
