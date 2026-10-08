package com.innovatex.auracast.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.innovatex.auracast.R
import com.innovatex.auracast.ui.i18n.appString
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovatex.auracast.components.SetupCheckViewModel
import com.innovatex.auracast.ui.theme.AppearanceMode
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings

@Composable
fun SetupCheckScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: SetupCheckViewModel = viewModel()
    val status = vm.status
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        vm.refresh(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refresh(context)
    }

    val bluetoothDetail = if (status.bluetoothReady) {
        appString(R.string.bluetooth_ready_detail)
    } else {
        appString(R.string.bluetooth_action_detail)
    }
    val locationDetail = if (status.locationGranted) {
        appString(R.string.location_ready_detail)
    } else {
        appString(R.string.location_action_detail)
    }
    val hearingDetail = if (status.hearingDeviceConnected) {
        appString(R.string.hearing_ready_detail)
    } else {
        appString(R.string.hearing_action_detail)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(appString(R.string.setup_step), style = MaterialTheme.typography.labelLarge)
            Text(
                appString(R.string.device_setup),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                appString(R.string.setup_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ReadinessCard(appString(R.string.bluetooth), status.bluetoothReady, bluetoothDetail)
            ReadinessCard(appString(R.string.location), status.locationGranted, locationDetail)
            ReadinessCard(appString(R.string.hearing_device), status.hearingDeviceConnected, hearingDetail)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Text(
                        appString(R.string.planning_without_device),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!status.bluetoothReady || !status.locationGranted) {
                OutlinedButton(
                    onClick = {
                        permissions.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(appString(R.string.allow_permissions))
                }
            }

            
            if (!status.hearingDeviceConnected) {
                Text(
                    appString(R.string.no_hearing_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!status.bluetoothReady || !status.locationGranted) {
                Text(
                    appString(R.string.bluetooth_location_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(appString(R.string.continue_route))
            }
            OutlinedButton(
                onClick = { vm.refresh(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(appString(R.string.refresh_checks))
            }
        }
    }
}

@Composable
private fun ReadinessCard(title: String, isSet: Boolean, detail: String) {
    val colours = MaterialTheme.colorScheme
    val isColourful = LocalAccessibilitySettings.current.appearance == AppearanceMode.COLOURFUL
    val backgroundColour = if (isColourful) {
        if (isSet) {
            colours.secondaryContainer
        } else {
            Color(0xFFFFF0CB)
        }
    } else {
        colours.surface
    }
    val indicatorColour = if (isColourful) {
        if (isSet) {
            colours.secondary
        } else {
            Color(0xFF825200)
        }
    } else {
        colours.onSurface
    }

    val stateText = if (isSet) {
        appString(R.string.ready)
    } else {
        appString(R.string.action_needed)
    }
    val accessibleStatus = appString(R.string.status_accessibility, title, stateText, detail)
    val statusIcon = if (isSet) {
        Icons.Default.CheckCircle
    } else {
        Icons.Default.WarningAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibleStatus },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = backgroundColour)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                tint = indicatorColour,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stateText,
                style = MaterialTheme.typography.labelSmall,
                color = indicatorColour
            )
        }
    }
}
