package com.innovatex.auracast.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel

import com.innovatex.auracast.components.SetupCheckViewModel
import com.innovatex.auracast.ui.theme.Muted
import com.innovatex.auracast.ui.theme.ReceivingGreen
import com.innovatex.auracast.ui.theme.SignalAmber
import com.innovatex.auracast.ui.theme.OnSignalAmber

@Composable
fun SetupCheckScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {}
) {
    // Get current context
    val context = LocalContext.current
    // Initialize view model
    val viewModel: SetupCheckViewModel = viewModel()

    // OS shows the permissions prompt
    // ActivityResult is general mechanism for any interaction with another screen
    val permLauncher = rememberLauncherForActivityResult(
        // Defines what goes in and out
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refresh(context)
    }

    // Status of the view model's current context
    val status = viewModel.status

    // Refreshes the context every time screen comes back to foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // Pass current context to refresh
        viewModel.refresh(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Before you start",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Three things need to be on for announcements to reach your hearing device.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted
        )

        Spacer(Modifier.height(20.dp))

        SetupCheckRow(
            isSet = status.bluetoothReady,
            title = "Bluetooth",
            detail = if (status.bluetoothReady) {
                "On"
            } else {
                "Turn Bluetooth on and allow this app to use it"
            }
        )
        HorizontalDivider()
        SetupCheckRow(
            isSet = status.locationGranted,
            title = "Location",
            detail = if (status.locationGranted) {
                "Allowed while using the app"
            } else {
                "Needed to know which stop you're at"
            }
        )
        HorizontalDivider()
        SetupCheckRow(
            isSet = status.hearingDeviceConnected,
            title = "Hearing device",
            detail = if (status.hearingDeviceConnected) {
                "Connected and ready"
            } else {
                "Connect LE Audio hearing aids or earbuds — your phone passes " +
                        "announcements to them, and can't play them through its own speaker."
            }
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                text = "Why a hearing device?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Auracast sends audio straight to your hearing aids. The phone's job is to pick the right channel, not to play the sound.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (status.allReady) {
                    onContinue()
                } else {
                    permLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (status.allReady) "Continue" else "Grant permissions")
        }
    }
}

@Composable
fun SetupCheckRow(
    isSet: Boolean,
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSet) ReceivingGreen else SignalAmber),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSet) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSet) Color.White else OnSignalAmber,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
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
    }
}