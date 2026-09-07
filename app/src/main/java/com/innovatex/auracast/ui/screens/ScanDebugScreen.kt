package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovatex.auracast.bluetooth.DiscoveredBroadcast
import com.innovatex.auracast.bluetooth.ScanViewModel
import com.innovatex.auracast.ui.theme.Muted

@Composable
fun ScanDebugScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ScanViewModel = viewModel()
    val found = viewModel.discovered.values.sortedByDescending { it.rssi }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Text("Scan debug", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (viewModel.isScanning) {
                "Scanning — ${found.size} found"
            } else {
                "Stopped — ${found.size} found"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (viewModel.isScanning) viewModel.stop() else viewModel.start(context)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (viewModel.isScanning) "Stop" else "Start")
            }
            OutlinedButton(onClick = { viewModel.clear() }) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (found.isEmpty()) {
            Text(
                text = "Nothing found yet. Check Bluetooth is on, location " +
                        "services are enabled, and a transmitter is broadcasting.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(found, key = { it.deviceAddress }) { broadcast ->
                BroadcastRow(broadcast)
            }
        }
    }
}

@Composable
private fun BroadcastRow(broadcast: DiscoveredBroadcast) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = broadcast.broadcastName ?: "(unnamed)",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "stop ${broadcast.metadata.stopIndex} - " +
                    "route ${broadcast.metadata.routeID} - " +
                    "dir ${broadcast.metadata.direction} - " +
                    "lang ${broadcast.metadata.language}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${broadcast.deviceAddress}   ${broadcast.rssi} dBm",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Muted
        )
    }
}