package com.innovatex.auracast.data

// Holds the status of bluetooth, location and hearing permissions
data class DeviceStatus(
    val bluetoothReady: Boolean = false,
    val locationGranted: Boolean = false,
    val hearingDeviceConnected: Boolean = false,
) {
    // Derived from all three bools
    val allReady: Boolean
        get() = bluetoothReady && locationGranted && hearingDeviceConnected
}