package com.innovatex.auracast.ui.screens

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
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
import androidx.compose.runtime.DisposableEffect
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
import com.innovatex.auracast.bluetooth.FmaUsbTransport
import com.innovatex.auracast.components.SetupCheckViewModel
import com.innovatex.auracast.ui.theme.Muted
import com.innovatex.auracast.ui.theme.OnSignalAmber
import com.innovatex.auracast.ui.theme.ReceivingGreen
import com.innovatex.auracast.ui.theme.SignalAmber

private const val ACTION_USB_PERMISSION =
    "com.innovatex.auracast.USB_PERMISSION"

@Composable
fun SetupCheckScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {}
) {

    val context =
        LocalContext.current

    val viewModel: SetupCheckViewModel =
        viewModel()

    val permLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {
            viewModel.refresh(context)
            requestFmaUsbPermission(context)
        }

    val status =
        viewModel.status

    DisposableEffect(context) {

        val usbPermissionReceiver =
            object : BroadcastReceiver() {

                override fun onReceive(
                    receiverContext: Context?,
                    intent: Intent?
                ) {

                    if (intent == null) {
                        return
                    }

                    if (intent.action == ACTION_USB_PERMISSION) {
                        viewModel.refresh(context)
                    }
                }
            }

        val filter =
            IntentFilter(
                ACTION_USB_PERMISSION
            )

        context.registerReceiver(
            usbPermissionReceiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(
                usbPermissionReceiver
            )
        }
    }

    LifecycleEventEffect(
        Lifecycle.Event.ON_RESUME
    ) {
        viewModel.refresh(context)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
    ) {

        Text(
            text = "Before you start",
            style =
                MaterialTheme.typography.headlineMedium
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text =
                "Make sure your receiver and hearing device are ready before starting the journey.",
            style =
                MaterialTheme.typography.bodyLarge,
            color = Muted
        )

        Spacer(
            Modifier.height(20.dp)
        )

        SetupCheckRow(
            isSet =
                status.bluetoothReady,
            title =
                "Bluetooth",
            detail =
                getBluetoothDetail(
                    status.bluetoothReady
                )
        )

        HorizontalDivider()

        SetupCheckRow(
            isSet =
                status.locationGranted,
            title =
                "Location",
            detail =
                getLocationDetail(
                    status.locationGranted
                )
        )

        HorizontalDivider()

        SetupCheckRow(
            isSet =
                status.fmaReceiverConnected,
            title =
                "FMA120 receiver",
            detail =
                getFmaReceiverDetail(
                    status.fmaReceiverConnected
                )
        )

        HorizontalDivider()

        SetupCheckRow(
            isSet =
                status.usbPermissionGranted,
            title =
                "USB access",
            detail =
                getUsbPermissionDetail(
                    status.usbPermissionGranted,
                    status.fmaReceiverConnected
                )
        )

        HorizontalDivider()

        val usbAudioReady =
            status.usbAudioInputReady &&
                    status.recordAudioGranted

        SetupCheckRow(
            isSet =
                usbAudioReady,
            title =
                "USB audio",
            detail =
                getUsbAudioDetail(
                    usbAudioInputReady =
                        status.usbAudioInputReady,
                    recordAudioGranted =
                        status.recordAudioGranted
                )
        )

        HorizontalDivider()

        SetupCheckRow(
            isSet =
                status.hearingDeviceConnected,
            title =
                "Hearing device",
            detail =
                getHearingDeviceDetail(
                    status.hearingDeviceConnected
                )
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    )
                    .padding(16.dp)
        ) {

            Text(
                text = "How audio works",
                style =
                    MaterialTheme.typography.bodyMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text =
                    "The FMA120 receives the Auracast announcement. " +
                            "The app automatically selects the correct stop " +
                            "and plays the received audio through your connected hearing device.",
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        Button(
            onClick = {

                if (status.allReady) {

                    onContinue()

                } else {

                    permLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            if (status.allReady) {

                Text(
                    text = "Continue"
                )

            } else {

                Text(
                    text = "Complete setup"
                )
            }
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
    ) {

        val backgroundColor: Color

        if (isSet) {
            backgroundColor =
                ReceivingGreen
        } else {
            backgroundColor =
                SignalAmber
        }

        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        backgroundColor
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            if (isSet) {

                Icon(
                    imageVector =
                        Icons.Default.Check,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(18.dp)
                )

            } else {

                Icon(
                    imageVector =
                        Icons.Default.Warning,
                    contentDescription =
                        null,
                    tint =
                        OnSignalAmber,
                    modifier =
                        Modifier.size(18.dp)
                )
            }
        }

        Spacer(
            Modifier.width(16.dp)
        )

        Column {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text = detail,
                style =
                    MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }
    }
}

private fun getBluetoothDetail(
    ready: Boolean
): String {

    if (ready) {
        return "On and ready"
    }

    return "Turn Bluetooth on and allow this app to use it"
}

private fun getLocationDetail(
    granted: Boolean
): String {

    if (granted) {
        return "Allowed while using the app"
    }

    return "Location permission is required for journey and stop detection"
}

private fun getFmaReceiverDetail(
    connected: Boolean
): String {

    if (connected) {
        return "Connected to your phone"
    }

    return "Connect the FMA120 receiver to your phone"
}

private fun getUsbPermissionDetail(
    granted: Boolean,
    receiverConnected: Boolean
): String {

    if (!receiverConnected) {
        return "Connect the FMA120 receiver first"
    }

    if (granted) {
        return "USB access granted"
    }

    return "Allow the app to access the FMA120 receiver"
}

private fun getUsbAudioDetail(
    usbAudioInputReady: Boolean,
    recordAudioGranted: Boolean
): String {

    if (!recordAudioGranted) {
        return "Allow audio permission so received announcements can be played"
    }

    if (!usbAudioInputReady) {
        return "FMA120 USB audio input is not available"
    }

    return "FMA120 audio input is ready"
}

private fun getHearingDeviceDetail(
    connected: Boolean
): String {

    if (connected) {
        return "Connected and ready"
    }

    return "Connect your hearing aids, earbuds, or compatible hearing device"
}

private fun requestFmaUsbPermission(
    context: Context
) {

    val usbManager =
        context.getSystemService(
            UsbManager::class.java
        )

    if (usbManager == null) {
        return
    }

    val fmaDevice =
        FmaUsbTransport.findDevice(
            context
        )

    if (fmaDevice == null) {
        return
    }

    if (usbManager.hasPermission(fmaDevice)) {
        return
    }

    val permissionIntent =
        Intent(
            ACTION_USB_PERMISSION
        )

    permissionIntent.setPackage(
        context.packageName
    )

    val pendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            permissionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

    usbManager.requestPermission(
        fmaDevice,
        pendingIntent
    )
}