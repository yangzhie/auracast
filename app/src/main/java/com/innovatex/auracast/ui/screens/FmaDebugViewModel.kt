package com.innovatex.auracast.ui.screens

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.innovatex.auracast.audio.UsbAudioRelay
import com.innovatex.auracast.bluetooth.Fma120ReceiverController
import com.innovatex.auracast.bluetooth.FmaReceiveState
import com.innovatex.auracast.bluetooth.FmaReceiverBroadcast
import com.innovatex.auracast.bluetooth.FmaUsbTransport

class FmaDebugViewModel : ViewModel() {

    var usbConnected by mutableStateOf(false)
        private set

    var usbPermissionGranted by mutableStateOf(false)
        private set

    var controlChannelOpen by mutableStateOf(false)
        private set

    var usbAudioDetected by mutableStateOf(false)
        private set

    var hearingOutputConnected by mutableStateOf(false)
        private set

    var recordAudioGranted by mutableStateOf(false)
        private set

    var audioTestRunning by mutableStateOf(false)
        private set

    var fmaSources by mutableStateOf<List<FmaReceiverBroadcast>>(
        emptyList()
    )
        private set

    var currentReceiveState by mutableStateOf<FmaReceiveState?>(
        null
    )
        private set

    var usbDevices by mutableStateOf<List<String>>(
        emptyList()
    )
        private set

    var statusMessage by mutableStateOf("")
        private set

    private var controller:
            Fma120ReceiverController? = null

    private var audioRelay:
            UsbAudioRelay? = null

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var scanRestartRunnable:
            Runnable? = null

    fun refresh(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        val usbManager =
            appContext.getSystemService(
                UsbManager::class.java
            )

        val fmaDevice =
            FmaUsbTransport.findDevice(
                appContext
            )

        if (fmaDevice != null) {

            usbConnected =
                true

            if (usbManager != null) {

                if (
                    usbManager.hasPermission(
                        fmaDevice
                    )
                ) {

                    usbPermissionGranted =
                        true

                } else {

                    usbPermissionGranted =
                        false
                }

            } else {

                usbPermissionGranted =
                    false
            }

        } else {

            usbConnected =
                false

            usbPermissionGranted =
                false

            controlChannelOpen =
                false
        }

        recordAudioGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        val audioManager =
            appContext.getSystemService(
                AudioManager::class.java
            )

        usbAudioDetected =
            false

        hearingOutputConnected =
            false

        if (audioManager != null) {

            val inputs =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_INPUTS
                )

            for (device in inputs) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_USB_DEVICE
                ) {

                    usbAudioDetected =
                        true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_USB_HEADSET
                ) {

                    usbAudioDetected =
                        true
                }
            }

            val outputs =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_OUTPUTS
                )

            for (device in outputs) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_HEARING_AID
                ) {

                    hearingOutputConnected =
                        true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_HEADSET
                ) {

                    hearingOutputConnected =
                        true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_SPEAKER
                ) {

                    hearingOutputConnected =
                        true
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                ) {

                    hearingOutputConnected =
                        true
                }

                if (
                    Build.VERSION.SDK_INT >= 37
                ) {

                    if (
                        device.type ==
                        AudioDeviceInfo.TYPE_BLE_HEARING_AID
                    ) {

                        hearingOutputConnected =
                            true
                    }
                }
            }
        }
    }

    fun listUsbDevices(
        context: Context
    ) {

        val usbManager =
            context.applicationContext
                .getSystemService(
                    UsbManager::class.java
                )

        if (usbManager == null) {

            usbDevices =
                emptyList()

            statusMessage =
                "USB service is unavailable."

            return
        }

        val devices =
            usbManager.deviceList.values

        val result =
            mutableListOf<String>()

        for (device in devices) {

            val description =
                "VID=0x${
                    device.vendorId
                        .toString(16)
                        .uppercase()
                } " +
                        "PID=0x${
                            device.productId
                                .toString(16)
                                .uppercase()
                        } " +
                        "Interfaces=${device.interfaceCount}"

            result.add(
                description
            )
        }

        usbDevices =
            result

        FmaUsbTransport.logUsbDevices(
            context
        )

        if (result.isEmpty()) {

            statusMessage =
                "No USB devices detected."

        } else {

            statusMessage =
                "${result.size} USB device(s) detected."
        }
    }

    fun requestUsbPermission(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        val usbManager =
            appContext.getSystemService(
                UsbManager::class.java
            )

        if (usbManager == null) {

            statusMessage =
                "USB service is unavailable."

            return
        }

        val fmaDevice =
            FmaUsbTransport.findDevice(
                appContext
            )

        if (fmaDevice == null) {

            statusMessage =
                "FMA120 receiver was not detected."

            return
        }

        if (
            usbManager.hasPermission(
                fmaDevice
            )
        ) {

            usbPermissionGranted =
                true

            openFma120(
                appContext
            )

            return
        }

        val intent =
            Intent(
                ACTION_USB_PERMISSION
            )

        intent.setPackage(
            appContext.packageName
        )

        val pendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        usbManager.requestPermission(
            fmaDevice,
            pendingIntent
        )

        statusMessage =
            "Waiting for USB permission."
    }

    fun onUsbPermissionResult(
        context: Context
    ) {

        refresh(
            context
        )

        if (usbPermissionGranted) {

            openFma120(
                context
            )

        } else {

            statusMessage =
                "USB permission was not granted."
        }
    }

    fun openFma120(
        context: Context
    ) {

        refresh(
            context
        )

        if (!usbConnected) {

            statusMessage =
                "Connect the FMA120 receiver first."

            return
        }

        if (!usbPermissionGranted) {

            requestUsbPermission(
                context
            )

            return
        }

        val oldController =
            controller

        if (oldController != null) {

            oldController.close()

            controller =
                null
        }

        val newController =
            Fma120ReceiverController(
                context =
                    context.applicationContext,

                onBroadcastFound = {
                        source ->

                    handleBroadcastFound(
                        source
                    )
                },

                onReceiveStateChanged = {
                        receiveState ->

                    handleReceiveState(
                        receiveState
                    )
                },

                onError = {
                        message ->

                    postStatus(
                        message
                    )
                }
            )

        val opened =
            newController.open()

        if (!opened) {

            controlChannelOpen =
                false

            controller =
                null

            statusMessage =
                "Unable to open FMA120 control channel."

            return
        }

        controller =
            newController

        controlChannelOpen =
            true

        statusMessage =
            "FMA120 control channel open."
    }

    fun scan() {

        val currentController =
            controller

        if (currentController == null) {

            statusMessage =
                "Open the FMA120 first."

            return
        }

        val previousRunnable =
            scanRestartRunnable

        if (previousRunnable != null) {

            mainHandler.removeCallbacks(
                previousRunnable
            )

            scanRestartRunnable =
                null
        }

        currentController.stopScan()

        statusMessage =
            "Restarting FMA120 scan..."

        val runnable =
            Runnable {

                val controllerAfterStop =
                    controller

                if (controllerAfterStop != null) {

                    controllerAfterStop.startScan()

                    statusMessage =
                        "Scanning for Auracast broadcasts."
                }

                scanRestartRunnable =
                    null
            }

        scanRestartRunnable =
            runnable

        mainHandler.postDelayed(
            runnable,
            SCAN_RESTART_DELAY_MS
        )
    }

    fun startScan() {

        scan()
    }

    fun stopScan() {

        val previousRunnable =
            scanRestartRunnable

        if (previousRunnable != null) {

            mainHandler.removeCallbacks(
                previousRunnable
            )

            scanRestartRunnable =
                null
        }

        val currentController =
            controller

        if (currentController == null) {

            statusMessage =
                "FMA120 is not open."

            return
        }

        currentController.stopScan()

        statusMessage =
            "FMA120 scan stopped."
    }

    fun clearScanResults() {

        fmaSources =
            emptyList()

        statusMessage =
            "Broadcast list cleared."
    }

    fun receive(
        source: FmaReceiverBroadcast
    ) {

        val currentController =
            controller

        if (currentController == null) {

            statusMessage =
                "Open the FMA120 first."

            return
        }

        currentReceiveState =
            null

        currentController.receive(
            source
        )

        statusMessage =
            "Connecting to ${source.broadcastName}."
    }

    fun stopReceiving() {

        val currentController =
            controller

        if (currentController == null) {

            statusMessage =
                "FMA120 is not open."

            return
        }

        currentController.stopReceiving()

        currentReceiveState =
            null

        statusMessage =
            "Broadcast reception stopped."
    }

    fun startAudioTest(
        context: Context
    ) {

        refresh(
            context
        )

        if (!recordAudioGranted) {

            statusMessage =
                "RECORD_AUDIO permission is required."

            return
        }

        val audioManager =
            context.applicationContext
                .getSystemService(
                    AudioManager::class.java
                )

        if (audioManager == null) {

            statusMessage =
                "Audio service is unavailable."

            return
        }

        var usbInput:
                AudioDeviceInfo? = null

        val inputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_INPUTS
            )

        for (device in inputs) {

            if (
                device.type ==
                AudioDeviceInfo.TYPE_USB_DEVICE
            ) {

                usbInput =
                    device

                break
            }

            if (
                device.type ==
                AudioDeviceInfo.TYPE_USB_HEADSET
            ) {

                usbInput =
                    device

                break
            }
        }

        if (usbInput == null) {

            statusMessage =
                "FMA120 USB audio input was not found."

            return
        }

        var hearingOutput:
                AudioDeviceInfo? = null

        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )

        for (device in outputs) {

            if (
                device.type ==
                AudioDeviceInfo.TYPE_HEARING_AID
            ) {

                hearingOutput =
                    device

                break
            }

            if (
                Build.VERSION.SDK_INT >= 37
            ) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_HEARING_AID
                ) {

                    hearingOutput =
                        device

                    break
                }
            }
        }

        if (hearingOutput == null) {

            for (device in outputs) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_HEADSET
                ) {

                    hearingOutput =
                        device

                    break
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_SPEAKER
                ) {

                    hearingOutput =
                        device

                    break
                }

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                ) {

                    hearingOutput =
                        device

                    break
                }
            }
        }

        if (hearingOutput == null) {

            statusMessage =
                "A hearing device or Bluetooth audio output was not found."

            return
        }

        val oldRelay =
            audioRelay

        if (oldRelay != null) {

            oldRelay.stop()

            audioRelay =
                null
        }

        val newRelay =
            UsbAudioRelay(
                context =
                    context.applicationContext,

                onError = {
                        message ->

                    mainHandler.post {

                        audioTestRunning =
                            false

                        statusMessage =
                            message
                    }
                }
            )

        val started =
            newRelay.start(
                usbInput =
                    usbInput,

                hearingOutput =
                    hearingOutput
            )

        if (!started) {

            newRelay.stop()

            audioTestRunning =
                false

            statusMessage =
                "Audio test could not be started."

            return
        }

        audioRelay =
            newRelay

        audioTestRunning =
            true

        statusMessage =
            "FMA120 USB audio relay started."
    }

    fun stopAudioTest() {

        val currentRelay =
            audioRelay

        if (currentRelay != null) {

            currentRelay.stop()
        }

        audioRelay =
            null

        audioTestRunning =
            false

        statusMessage =
            "Audio test stopped."
    }

    fun close() {

        val previousRunnable =
            scanRestartRunnable

        if (previousRunnable != null) {

            mainHandler.removeCallbacks(
                previousRunnable
            )

            scanRestartRunnable =
                null
        }

        val currentRelay =
            audioRelay

        if (currentRelay != null) {

            currentRelay.stop()
        }

        audioRelay =
            null

        audioTestRunning =
            false

        val currentController =
            controller

        if (currentController != null) {

            currentController.stopScan()

            currentController.stopReceiving()

            currentController.close()
        }

        controller =
            null

        controlChannelOpen =
            false

        currentReceiveState =
            null
    }

    private fun handleBroadcastFound(
        source: FmaReceiverBroadcast
    ) {

        val updated =
            fmaSources.toMutableList()

        var existingIndex =
            -1

        for (index in updated.indices) {

            val existing =
                updated[index]

            val sameAddress =
                normalizeAddress(
                    existing.address
                ) ==
                        normalizeAddress(
                            source.address
                        )

            val sameIds =
                existing.broadcastIDs ==
                        source.broadcastIDs

            if (
                sameAddress &&
                sameIds
            ) {

                existingIndex =
                    index

                break
            }
        }

        if (existingIndex >= 0) {

            updated[existingIndex] =
                source

        } else {

            updated.add(
                source
            )
        }

        updated.sortByDescending {
            it.rssi
        }

        fmaSources =
            updated
    }

    private fun handleReceiveState(
        receiveState: FmaReceiveState
    ) {

        currentReceiveState =
            receiveState

        if (
            receiveState.needsBroadcastCode
        ) {

            val currentController =
                controller

            if (currentController != null) {

                currentController.provideBroadcastCode(
                    sourceId =
                        receiveState.sourceId,

                    code =
                        BROADCAST_CODE
                )

                statusMessage =
                    "Broadcast Code sent."

                return
            }
        }

        if (
            receiveState.syncFailed
        ) {

            statusMessage =
                "FMA120 synchronization failed."

            return
        }

        if (
            receiveState.isStreaming
        ) {

            statusMessage =
                "FMA120 is receiving the Auracast broadcast."

            return
        }

        statusMessage =
            "FMA120 receiver state updated."
    }

    private fun normalizeAddress(
        address: String
    ): String {

        return address
            .replace(
                ":",
                ""
            )
            .replace(
                "-",
                ""
            )
            .trim()
            .uppercase()
    }

    private fun postStatus(
        message: String
    ) {

        mainHandler.post {

            statusMessage =
                message
        }
    }

    override fun onCleared() {

        close()

        super.onCleared()
    }

    companion object {

        const val ACTION_USB_PERMISSION =
            "com.innovatex.auracast.FMA_DEBUG_USB_PERMISSION"

        private const val BROADCAST_CODE =
            "AURA86DEMO2026"

        private const val SCAN_RESTART_DELAY_MS =
            300L
    }
}