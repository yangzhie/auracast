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

    var autoModeEnabled by mutableStateOf(false)
        private set

    var activeAutoSource by mutableStateOf<FmaReceiverBroadcast?>(
        null
    )
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

    private var applicationContext:
            Context? = null

    private var controller:
            Fma120ReceiverController? = null

    private var audioRelay:
            UsbAudioRelay? = null

    private var pendingAutoSource:
            FmaReceiverBroadcast? = null

    private var autoRecoveryInProgress =
        false

    private var autoRestartRunnable:
            Runnable? = null

    private var audioRetryRunnable:
            Runnable? = null

    private var audioRetryCount =
        0

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    fun refresh(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        applicationContext =
            appContext

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

                usbPermissionGranted =
                    usbManager.hasPermission(
                        fmaDevice
                    )

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

        applicationContext =
            appContext


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

        val appContext =
            context.applicationContext

        applicationContext =
            appContext


        refresh(
            appContext
        )


        if (!usbConnected) {

            statusMessage =
                "Connect the FMA120 receiver first."

            return
        }


        if (!usbPermissionGranted) {

            requestUsbPermission(
                appContext
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
                    appContext,

                onBroadcastFound = {
                        source ->

                    mainHandler.post {

                        handleBroadcastFound(
                            source
                        )
                    }
                },

                onReceiveStateChanged = {
                        receiveState ->

                    mainHandler.post {

                        handleReceiveState(
                            receiveState
                        )
                    }
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


        if (autoModeEnabled) {

            fmaSources =
                emptyList()

            pendingAutoSource =
                null

            activeAutoSource =
                null

            currentReceiveState =
                null


            newController.startScan()


            statusMessage =
                "Automatic mode active. Scanning for Auracast transmitters."

        } else {

            statusMessage =
                "FMA120 control channel open."
        }
    }


    fun startAutoMode(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        applicationContext =
            appContext


        autoModeEnabled =
            true

        activeAutoSource =
            null

        pendingAutoSource =
            null

        currentReceiveState =
            null

        autoRecoveryInProgress =
            false


        cancelAutoRestart()

        cancelAudioRetry()

        stopAudioRelayInternal()


        refresh(
            appContext
        )


        if (!usbConnected) {

            statusMessage =
                "Connect the FMA120 receiver."

            return
        }


        if (!usbPermissionGranted) {

            requestUsbPermission(
                appContext
            )

            return
        }


        val currentController =
            controller


        if (currentController == null) {

            openFma120(
                appContext
            )

            return
        }


        fmaSources =
            emptyList()


        currentController.startScan()


        statusMessage =
            "Automatic mode active. Scanning for Auracast transmitters."
    }


    fun stopAutoMode() {

        autoModeEnabled =
            false

        autoRecoveryInProgress =
            false


        cancelAutoRestart()

        cancelAudioRetry()

        stopAudioRelayInternal()


        val currentController =
            controller


        if (currentController != null) {

            currentController.stopReceiving()

            currentController.stopScan()
        }


        activeAutoSource =
            null

        pendingAutoSource =
            null

        currentReceiveState =
            null


        statusMessage =
            "Automatic mode stopped."
    }


    fun scan() {

        val currentController =
            controller


        if (currentController == null) {

            statusMessage =
                "Open the FMA120 first."

            return
        }


        fmaSources =
            emptyList()


        currentController.startScan()


        statusMessage =
            "Scanning for Auracast broadcasts."
    }


    fun startScan() {

        scan()
    }


    fun stopScan() {

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

        activeAutoSource =
            null

        pendingAutoSource =
            null


        stopAudioRelayInternal()


        statusMessage =
            "Broadcast reception stopped."
    }


    fun startAudioTest(
        context: Context
    ) {

        applicationContext =
            context.applicationContext


        startAudioRelay(
            context =
                context,

            automatic =
                false
        )
    }


    fun stopAudioTest() {

        cancelAudioRetry()

        stopAudioRelayInternal()


        statusMessage =
            "Audio test stopped."
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


        if (
            existingIndex >=
            0
        ) {

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


        if (!autoModeEnabled) {

            return
        }


        if (autoRecoveryInProgress) {

            return
        }


        if (
            activeAutoSource != null
        ) {

            return
        }


        if (
            pendingAutoSource != null
        ) {

            return
        }


        val currentController =
            controller


        if (currentController == null) {

            statusMessage =
                "FMA120 controller is unavailable."

            return
        }


        pendingAutoSource =
            source


        currentReceiveState =
            null


        statusMessage =
            "Automatically connecting to ${source.broadcastName}."


        currentController.receive(
            source
        )
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
            }


            return
        }


        if (
            receiveState.syncFailed
        ) {

            if (autoModeEnabled) {

                recoverAutomaticConnection()

                return
            }


            statusMessage =
                "FMA120 synchronization failed."

            return
        }


        if (
            receiveState.isStreaming
        ) {

            autoRecoveryInProgress =
                false


            if (autoModeEnabled) {

                val pending =
                    pendingAutoSource


                if (pending != null) {

                    activeAutoSource =
                        pending
                }


                pendingAutoSource =
                    null


                startAutomaticAudio()


                val active =
                    activeAutoSource


                if (active != null) {

                    statusMessage =
                        "Automatically connected to ${active.broadcastName}."

                } else {

                    statusMessage =
                        "Auracast broadcast is streaming."
                }


                return
            }


            statusMessage =
                "FMA120 is receiving the Auracast broadcast."

            return
        }


        statusMessage =
            "FMA120 receiver state updated."
    }


    private fun recoverAutomaticConnection() {

        if (!autoModeEnabled) {

            return
        }


        if (autoRecoveryInProgress) {

            return
        }


        autoRecoveryInProgress =
            true


        cancelAudioRetry()

        stopAudioRelayInternal()


        activeAutoSource =
            null

        pendingAutoSource =
            null

        currentReceiveState =
            null


        val currentController =
            controller


        if (currentController == null) {

            autoRecoveryInProgress =
                false

            statusMessage =
                "FMA120 controller is unavailable."

            return
        }


        currentController.stopReceiving()

        currentController.stopScan()


        statusMessage =
            "Connection failed. Searching again."


        val runnable =
            Runnable {

                autoRestartRunnable =
                    null


                if (!autoModeEnabled) {

                    autoRecoveryInProgress =
                        false

                    return@Runnable
                }


                val activeController =
                    controller


                if (activeController == null) {

                    autoRecoveryInProgress =
                        false

                    return@Runnable
                }


                fmaSources =
                    emptyList()


                autoRecoveryInProgress =
                    false


                activeController.startScan()


                statusMessage =
                    "Searching again for an Auracast transmitter."
            }


        autoRestartRunnable =
            runnable


        mainHandler.postDelayed(
            runnable,
            AUTO_RESCAN_DELAY_MS
        )
    }


    private fun startAutomaticAudio() {

        cancelAudioRetry()


        audioRetryCount =
            0


        attemptAutomaticAudio()
    }


    private fun attemptAutomaticAudio() {

        if (!autoModeEnabled) {

            return
        }


        val context =
            applicationContext


        if (context == null) {

            statusMessage =
                "Application context is unavailable."

            return
        }


        val started =
            startAudioRelay(
                context =
                    context,

                automatic =
                    true
            )


        if (started) {

            return
        }


        if (
            audioRetryCount >=
            MAX_AUDIO_RETRIES
        ) {

            statusMessage =
                "Auracast is connected, but the audio relay could not start."

            return
        }


        audioRetryCount =
            audioRetryCount + 1


        val runnable =
            Runnable {

                audioRetryRunnable =
                    null


                attemptAutomaticAudio()
            }


        audioRetryRunnable =
            runnable


        mainHandler.postDelayed(
            runnable,
            AUDIO_RETRY_DELAY_MS
        )
    }


    private fun startAudioRelay(
        context: Context,
        automatic: Boolean
    ): Boolean {

        val appContext =
            context.applicationContext


        refresh(
            appContext
        )


        if (!recordAudioGranted) {

            statusMessage =
                "RECORD_AUDIO permission is required."

            return false
        }


        val audioManager =
            appContext.getSystemService(
                AudioManager::class.java
            )


        if (audioManager == null) {

            statusMessage =
                "Audio service is unavailable."

            return false
        }


        val usbInput =
            findUsbAudioInput(
                audioManager
            )


        if (usbInput == null) {

            if (automatic) {

                statusMessage =
                    "Auracast connected. Waiting for FMA120 USB audio."

            } else {

                statusMessage =
                    "FMA120 USB audio input was not found."
            }


            return false
        }


        val hearingOutput =
            findHearingOutput(
                audioManager
            )


        if (hearingOutput == null) {

            statusMessage =
                "A hearing device or Bluetooth audio output was not found."

            return false
        }


        stopAudioRelayInternal()


        val newRelay =
            UsbAudioRelay(
                context =
                    appContext,

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


            if (!automatic) {

                statusMessage =
                    "Audio test could not be started."
            }


            return false
        }


        audioRelay =
            newRelay


        audioTestRunning =
            true


        if (!automatic) {

            statusMessage =
                "FMA120 USB audio relay started."
        }


        return true
    }


    private fun findUsbAudioInput(
        audioManager: AudioManager
    ): AudioDeviceInfo? {

        val inputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_INPUTS
            )


        for (device in inputs) {

            if (
                device.type ==
                AudioDeviceInfo.TYPE_USB_DEVICE
            ) {

                return device
            }


            if (
                device.type ==
                AudioDeviceInfo.TYPE_USB_HEADSET
            ) {

                return device
            }
        }


        return null
    }


    private fun findHearingOutput(
        audioManager: AudioManager
    ): AudioDeviceInfo? {

        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )


        for (device in outputs) {

            if (
                device.type ==
                AudioDeviceInfo.TYPE_HEARING_AID
            ) {

                return device
            }


            if (
                Build.VERSION.SDK_INT >= 37
            ) {

                if (
                    device.type ==
                    AudioDeviceInfo.TYPE_BLE_HEARING_AID
                ) {

                    return device
                }
            }
        }


        for (device in outputs) {

            if (
                device.type ==
                AudioDeviceInfo.TYPE_BLE_HEADSET
            ) {

                return device
            }


            if (
                device.type ==
                AudioDeviceInfo.TYPE_BLE_SPEAKER
            ) {

                return device
            }


            if (
                device.type ==
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            ) {

                return device
            }
        }


        return null
    }


    private fun stopAudioRelayInternal() {

        val currentRelay =
            audioRelay


        if (currentRelay != null) {

            currentRelay.stop()
        }


        audioRelay =
            null


        audioTestRunning =
            false
    }


    private fun cancelAutoRestart() {

        val runnable =
            autoRestartRunnable


        if (runnable != null) {

            mainHandler.removeCallbacks(
                runnable
            )
        }


        autoRestartRunnable =
            null
    }


    private fun cancelAudioRetry() {

        val runnable =
            audioRetryRunnable


        if (runnable != null) {

            mainHandler.removeCallbacks(
                runnable
            )
        }


        audioRetryRunnable =
            null


        audioRetryCount =
            0
    }


    fun close() {

        autoModeEnabled =
            false


        cancelAutoRestart()

        cancelAudioRetry()

        stopAudioRelayInternal()


        val currentController =
            controller


        if (currentController != null) {

            currentController.stopReceiving()

            currentController.stopScan()

            currentController.close()
        }


        controller =
            null


        controlChannelOpen =
            false


        currentReceiveState =
            null


        activeAutoSource =
            null


        pendingAutoSource =
            null


        fmaSources =
            emptyList()
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

        private const val AUTO_RESCAN_DELAY_MS =
            500L

        private const val AUDIO_RETRY_DELAY_MS =
            500L

        private const val MAX_AUDIO_RETRIES =
            10
    }
}