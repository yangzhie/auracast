package com.innovatex.auracast.components

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.innovatex.auracast.bluetooth.FmaUsbTransport
import com.innovatex.auracast.data.DeviceStatus

class SetupCheckViewModel : ViewModel() {

    var status by mutableStateOf(DeviceStatus())
        private set

    fun refresh(context: Context) {

        val hasBluetoothConnectPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

        val hasBluetoothScanPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val hasRecordAudioPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        val bluetoothManager =
            context.getSystemService(
                BluetoothManager::class.java
            )

        var bluetoothEnabled = false

        if (bluetoothManager != null) {

            val bluetoothAdapter =
                bluetoothManager.adapter

            if (bluetoothAdapter != null) {

                if (bluetoothAdapter.isEnabled) {
                    bluetoothEnabled = true
                }
            }
        }

        val bluetoothReady =
            hasBluetoothConnectPermission &&
                    hasBluetoothScanPermission &&
                    bluetoothEnabled

        val usbManager =
            context.getSystemService(
                UsbManager::class.java
            )

        val fmaDevice =
            FmaUsbTransport.findDevice(context)

        var fmaReceiverConnected = false
        var usbPermissionGranted = false

        if (fmaDevice != null) {

            fmaReceiverConnected = true

            if (usbManager != null) {

                if (usbManager.hasPermission(fmaDevice)) {
                    usbPermissionGranted = true
                }
            }
        }

        val audioManager =
            context.getSystemService(
                AudioManager::class.java
            )

        var hasUsbAudioInput = false
        var hasHearingOutput = false

        if (audioManager != null) {

            val inputDevices =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_INPUTS
                )

            for (device in inputDevices) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_USB_DEVICE
                ) {
                    hasUsbAudioInput = true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_USB_HEADSET
                ) {
                    hasUsbAudioInput = true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_USB_ACCESSORY
                ) {
                    hasUsbAudioInput = true
                }
            }

            val outputDevices =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_OUTPUTS
                )

            for (device in outputDevices) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_HEADSET
                ) {
                    hasHearingOutput = true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_HEARING_AID
                ) {
                    hasHearingOutput = true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_SPEAKER
                ) {
                    hasHearingOutput = true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                ) {
                    hasHearingOutput = true
                }

                if (Build.VERSION.SDK_INT >= 37) {

                    if (
                        device.type ==
                        AudioDeviceInfo.TYPE_BLE_HEARING_AID
                    ) {
                        hasHearingOutput = true
                    }
                }
            }
        }

        status =
            DeviceStatus(
                bluetoothReady =
                    bluetoothReady,

                locationGranted =
                    hasLocationPermission,

                fmaReceiverConnected =
                    fmaReceiverConnected,

                usbPermissionGranted =
                    usbPermissionGranted,

                usbAudioInputReady =
                    hasUsbAudioInput,

                recordAudioGranted =
                    hasRecordAudioPermission,

                hearingDeviceConnected =
                    hasHearingOutput
            )
    }
}