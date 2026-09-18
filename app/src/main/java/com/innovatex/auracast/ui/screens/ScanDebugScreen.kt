package com.innovatex.auracast.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovatex.auracast.bluetooth.DiscoveredBroadcast
import com.innovatex.auracast.ui.screens.FmaDebugViewModel
import com.innovatex.auracast.bluetooth.FmaReceiverBroadcast
import com.innovatex.auracast.bluetooth.FmaSourceResolver
import com.innovatex.auracast.bluetooth.ScanViewModel
import com.innovatex.auracast.ui.theme.Muted

@Composable
fun ScanDebugScreen(
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current

    val scanViewModel: ScanViewModel =
        viewModel()

    val fmaViewModel: FmaDebugViewModel =
        viewModel()

    val androidBroadcasts =
        scanViewModel.discovered.values
            .sortedByDescending {
                it.rssi
            }

    val fmaBroadcasts =
        fmaViewModel.fmaSources
            .sortedByDescending {
                it.rssi
            }

    var selectedFmaSource by
    remember {
        mutableStateOf<FmaReceiverBroadcast?>(
            null
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
    ) {

        Text(
            text = "Auracast debug",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text =
                "Compare Android BF scanning with FMA120 receiver scanning.",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                Muted
        )

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            text = "ANDROID SCANNER",
            style =
                MaterialTheme.typography.labelLarge,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {

                    if (scanViewModel.isScanning) {

                        scanViewModel.stop()

                    } else {

                        scanViewModel.start(
                            context
                        )
                    }
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                if (scanViewModel.isScanning) {

                    Text(
                        text = "Stop Android scan"
                    )

                } else {

                    Text(
                        text = "Start Android scan"
                    )
                }
            }

            OutlinedButton(
                onClick = {

                    scanViewModel.clear()
                }
            ) {

                Text(
                    text = "Clear"
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        if (androidBroadcasts.isEmpty()) {

            Text(
                text =
                    "No InnovateX transmitters found by Android.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    Muted
            )

        } else {

            for (broadcast in androidBroadcasts) {

                val matchedFmaSource =
                    FmaSourceResolver.resolve(
                        androidBroadcast =
                            broadcast,
                        fmaBroadcasts =
                            fmaBroadcasts
                    )

                AndroidBroadcastRow(
                    broadcast =
                        broadcast,
                    matched =
                        matchedFmaSource != null
                )

                Spacer(
                    Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            Modifier.height(20.dp)
        )

        HorizontalDivider()

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            text = "FMA120 RECEIVER",
            style =
                MaterialTheme.typography.labelLarge,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = {

                    fmaViewModel.listUsbDevices(
                        context
                    )
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "USB devices"
                )
            }

            Button(
                onClick = {

                    fmaViewModel.openFma120(
                        context
                    )
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Open FMA120"
                )
            }
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {

                    fmaViewModel.startScan()
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "FMA scan"
                )
            }

            OutlinedButton(
                onClick = {

                    fmaViewModel.stopScan()
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Stop FMA scan"
                )
            }
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                enabled =
                    selectedFmaSource != null,
                onClick = {

                    val selected =
                        selectedFmaSource

                    if (selected != null) {

                        fmaViewModel.receive(
                            selected
                        )
                    }
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Receive selected"
                )
            }

            OutlinedButton(
                onClick = {

                    fmaViewModel.stopReceiving()
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Stop receiving"
                )
            }
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Button(
            onClick = {

                fmaViewModel.startAudioTest(
                    context
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Audio test"
            )
        }

        Spacer(
            Modifier.height(12.dp)
        )

        if (fmaViewModel.statusMessage.isNotEmpty()) {

            Text(
                text =
                    fmaViewModel.statusMessage,
                style =
                    MaterialTheme.typography.bodySmall,
                fontFamily =
                    FontFamily.Monospace,
                color =
                    Muted
            )

            Spacer(
                Modifier.height(12.dp)
            )
        }

        if (fmaViewModel.usbDevices.isNotEmpty()) {

            Text(
                text = "USB devices detected",
                style =
                    MaterialTheme.typography.titleSmall
            )

            Spacer(
                Modifier.height(4.dp)
            )

            for (device in fmaViewModel.usbDevices) {

                Text(
                    text = device,
                    style =
                        MaterialTheme.typography.bodySmall,
                    fontFamily =
                        FontFamily.Monospace
                )
            }

            Spacer(
                Modifier.height(12.dp)
            )
        }

        if (fmaBroadcasts.isEmpty()) {

            Text(
                text =
                    "No Auracast broadcasts found by the FMA120.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    Muted
            )

        } else {

            for (source in fmaBroadcasts) {

                val matchingAndroidBroadcast =
                    findMatchingAndroidBroadcast(
                        source =
                            source,
                        androidBroadcasts =
                            androidBroadcasts
                    )

                val isSelected =
                    selectedFmaSource ==
                            source

                FmaBroadcastRow(
                    source =
                        source,
                    matched =
                        matchingAndroidBroadcast != null,
                    selected =
                        isSelected,
                    onClick = {

                        selectedFmaSource =
                            source
                    }
                )

                Spacer(
                    Modifier.height(8.dp)
                )
            }
        }

        val receiveState =
            fmaViewModel.currentReceiveState

        if (receiveState != null) {

            Spacer(
                Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text = "RECEIVER STATE",
                style =
                    MaterialTheme.typography.labelLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    "Source ID: ${receiveState.sourceId}",
                fontFamily =
                    FontFamily.Monospace
            )

            Text(
                text =
                    "Broadcast IDs: ${receiveState.broadcastIDs}",
                fontFamily =
                    FontFamily.Monospace
            )

            Text(
                text =
                    "Sync state: ${
                        formatHex(
                            receiveState.syncState,
                            2
                        )
                    }",
                fontFamily =
                    FontFamily.Monospace
            )

            Text(
                text =
                    "Encryption state: ${
                        formatHex(
                            receiveState.encryptionState,
                            2
                        )
                    }",
                fontFamily =
                    FontFamily.Monospace
            )

            Text(
                text =
                    "BIS state: ${
                        formatHex(
                            receiveState.bisState,
                            2
                        )
                    }",
                fontFamily =
                    FontFamily.Monospace
            )

            Spacer(
                Modifier.height(6.dp)
            )

            if (receiveState.isStreaming) {

                Text(
                    text = "STREAMING ✓",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

            } else {

                Text(
                    text = "Not streaming",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        Muted
                )
            }
        }

        Spacer(
            Modifier.height(24.dp)
        )
    }
}

@Composable
private fun AndroidBroadcastRow(
    broadcast: DiscoveredBroadcast,
    matched: Boolean
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                )
                .padding(12.dp)
    ) {

        Text(
            text =
                broadcast.broadcastName
                    ?: "(unnamed)",
            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text =
                "Route ${broadcast.metadata.routeID}",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "Stop ${broadcast.metadata.stopIndex}",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "Direction ${broadcast.metadata.direction}",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "Language ${broadcast.metadata.language}",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text =
                broadcast.deviceAddress,
            style =
                MaterialTheme.typography.bodySmall,
            fontFamily =
                FontFamily.Monospace,
            color =
                Muted
        )

        Text(
            text =
                "${broadcast.rssi} dBm",
            style =
                MaterialTheme.typography.bodySmall,
            fontFamily =
                FontFamily.Monospace,
            color =
                Muted
        )

        if (matched) {

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text = "MATCH ✓",
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FmaBroadcastRow(
    source: FmaReceiverBroadcast,
    matched: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {

    val containerColor =
        if (selected) {

            MaterialTheme
                .colorScheme
                .secondaryContainer

        } else {

            MaterialTheme
                .colorScheme
                .surfaceVariant
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(
                    containerColor
                )
                .clickable {

                    onClick()
                }
                .padding(12.dp)
    ) {

        Text(
            text =
                source.broadcastName,
            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text =
                "Address ${source.address}",
            style =
                MaterialTheme.typography.bodySmall,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "SID ${
                    formatHex(
                        source.advertisingSid,
                        2
                    )
                }",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "Broadcast ID ${
                    formatHex(
                        source.broadcastId,
                        6
                    )
                }",
            style =
                MaterialTheme.typography.bodyMedium,
            fontFamily =
                FontFamily.Monospace
        )

        Text(
            text =
                "${source.rssi} dBm",
            style =
                MaterialTheme.typography.bodySmall,
            fontFamily =
                FontFamily.Monospace,
            color =
                Muted
        )

        if (matched) {

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text = "MATCH ✓",
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight =
                    FontWeight.Bold
            )
        }

        if (selected) {

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text = "Selected",
                style =
                    MaterialTheme.typography.bodySmall,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

private fun findMatchingAndroidBroadcast(
    source: FmaReceiverBroadcast,
    androidBroadcasts:
    List<DiscoveredBroadcast>
): DiscoveredBroadcast? {

    for (broadcast in androidBroadcasts) {

        val resolved =
            FmaSourceResolver.resolve(
                androidBroadcast =
                    broadcast,
                fmaBroadcasts =
                    listOf(source)
            )

        if (resolved != null) {

            return broadcast
        }
    }

    return null
}

private fun formatHex(
    value: Int,
    width: Int
): String {

    return value
        .toString(16)
        .uppercase()
        .padStart(
            width,
            '0'
        )
}