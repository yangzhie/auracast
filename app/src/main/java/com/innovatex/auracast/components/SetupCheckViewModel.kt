package com.innovatex.auracast.components

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

import com.innovatex.auracast.data.DeviceStatus

// ViewModel for permissions
class SetupCheckViewModel : ViewModel() {
    // State for remembering app-wide permissions
    var status by mutableStateOf(DeviceStatus())
        private set // out-of-scope funs can read, but not edit

    // Context - OS-level handling by the application
    // Pushes a fresh value into status for all permissions
    fun refresh(context: Context) {
        // Check: bluetooth is granted to app
        val hasBluetoothPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        // Check: device has granted location tracking to app
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Context manager for status
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

        // Modify status state respective to DeviceStatus
        status = DeviceStatus(
            // Device must give app bluetooth perms + have bluetooth
            bluetoothReady = hasBluetoothPermission && bluetoothManager?.adapter?.isEnabled == true,
            locationGranted = hasLocationPermission,
            // TODO: re-work later
            hearingDeviceConnected = false
        )
    }
}