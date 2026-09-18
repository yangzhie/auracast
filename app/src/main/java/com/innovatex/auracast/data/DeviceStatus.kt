package com.innovatex.auracast.data

data class DeviceStatus(
    val bluetoothReady: Boolean = false,
    val locationGranted: Boolean = false,
    val fmaReceiverConnected: Boolean = false,
    val usbPermissionGranted: Boolean = false,
    val usbAudioInputReady: Boolean = false,
    val recordAudioGranted: Boolean = false,
    val hearingDeviceConnected: Boolean = false
) {

    val allReady: Boolean
        get() {
            if (!bluetoothReady) {
                return false
            }

            if (!locationGranted) {
                return false
            }

            if (!fmaReceiverConnected) {
                return false
            }

            if (!usbPermissionGranted) {
                return false
            }

            if (!usbAudioInputReady) {
                return false
            }

            if (!recordAudioGranted) {
                return false
            }

            if (!hearingDeviceConnected) {
                return false
            }

            return true
        }
}