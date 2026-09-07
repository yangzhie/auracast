package com.innovatex.auracast.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ScanViewModel : ViewModel() {
    // State for a map of discovered broadcasts
    var discovered by mutableStateOf<Map<String, DiscoveredBroadcast>>(emptyMap())
        private set

    // State for the scan flag
    var isScanning by mutableStateOf(false)
        private set // Prevent any screen from modifying directly

    // Scanner
    private var scanner: BroadcastScanner? = null

    // Manually age transmitter-fed entries
    private val staleTransmitter = 5_000L

    // Uses scanner's bg thread - rebuilds the map
    @SuppressLint("MissingPermission")
    private fun onFound(
        address: String,
        name: String?,
        rssi: Int,
        metadata: BroadcastMetadata
    ) {
        // Time at the moment
        val timeNow = System.currentTimeMillis()

        // Create the entry
        val entry = DiscoveredBroadcast(
            deviceAddress = address,
            broadcastName = name,
            rssi = rssi,
            metadata = metadata,
            lastSeenMillis = timeNow
        )

        // Create mutable map copy of discovered devices
        val updated = discovered.toMutableMap()
        // Update entry of the copied map
        updated[address] = entry

        // Remove all stale entries
        updated.entries.removeAll { timeNow - it.value.lastSeenMillis > staleTransmitter }

        // Update the discovered map
        discovered = updated
    }

    // Start the scanner
    fun start(context: Context) {
        // Check: if it's already scanning
        if (isScanning) {
            return
        }

        // Create a new scanner (needs a context)
        val newScanner = BroadcastScanner(
            context = context.applicationContext,
            onBroadcastFound = { result, metadata ->
                onFound(result.device.address, result.scanRecord?.deviceName, result.rssi, metadata)
            }
        )

        newScanner.start()
        scanner = newScanner
        isScanning = true
    }

    // Stop the scanner
    fun stop() {
        scanner?.stop()
        scanner = null
        isScanning = false
    }

    // Clear discovered state's map
    fun clear() {
        discovered = emptyMap()
    }

    override fun onCleared() {
        stop()
    }
}