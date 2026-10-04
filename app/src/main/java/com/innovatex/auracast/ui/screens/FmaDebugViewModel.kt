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
import android.util.Log
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


    // =========================================================
    // UI STATE
    // =========================================================

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


    // =========================================================
    // INTERNAL OBJECTS
    // =========================================================

    private var controller:
            Fma120ReceiverController? = null


    private var audioRelay:
            UsbAudioRelay? = null


    private var applicationContext:
            Context? = null


    /*
     * Source for which receive(source) has been sent,
     * but STREAMING has not yet been confirmed.
     */
    private var pendingAutoSource:
            FmaReceiverBroadcast? = null


    /*
     * Prevent multiple reconnect timers.
     */
    private var autoRescanRunnable:
            Runnable? = null


    /*
     * Automatic audio startup retry.
     */
    private var audioRetryRunnable:
            Runnable? = null


    private var audioRetryCount =
        0


    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    // =========================================================
    // REFRESH DEVICE STATUS
    // =========================================================

    fun refresh(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        applicationContext =
            appContext


        // -----------------------------------------------------
        // USB / FMA120
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // RECORD_AUDIO permission
        // -----------------------------------------------------

        recordAudioGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED


        // -----------------------------------------------------
        // Android audio devices
        // -----------------------------------------------------

        val audioManager =
            appContext.getSystemService(
                AudioManager::class.java
            )


        usbAudioDetected =
            false


        hearingOutputConnected =
            false


        if (audioManager != null) {


            // -------------------------------------------------
            // FMA120 USB audio input
            // -------------------------------------------------

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


            // -------------------------------------------------
            // Hearing / Bluetooth output
            // -------------------------------------------------

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


    // =========================================================
    // USB DEVICE LIST
    // =========================================================

    fun listUsbDevices(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        val usbManager =
            appContext.getSystemService(
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


            val vendorId =
                device.vendorId
                    .toString(16)
                    .uppercase()


            val productId =
                device.productId
                    .toString(16)
                    .uppercase()


            val description =
                "VID=0x$vendorId " +
                        "PID=0x$productId " +
                        "Interfaces=${device.interfaceCount}"


            result.add(
                description
            )
        }


        usbDevices =
            result


        FmaUsbTransport.logUsbDevices(
            appContext
        )


        if (result.isEmpty()) {

            statusMessage =
                "No USB devices detected."

        } else {

            statusMessage =
                "${result.size} USB device(s) detected."
        }
    }


    // =========================================================
    // USB PERMISSION
    // =========================================================

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


        /*
         * Permission may already have been granted
         * since the previous refresh.
         */
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


            /*
             * Continue automatic startup.
             */
            if (autoModeEnabled) {

                startAutomaticScan()
            }


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


        Log.i(
            TAG,
            "AUTO: waiting for USB permission"
        )
    }


    // =========================================================
    // USB PERMISSION RESULT
    // =========================================================

    fun onUsbPermissionResult(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        applicationContext =
            appContext


        refresh(
            appContext
        )


        Log.i(
            TAG,
            "USB permission result: granted=$usbPermissionGranted"
        )


        if (!usbPermissionGranted) {

            statusMessage =
                "USB permission was not granted."


            return
        }


        /*
         * Open FMA120 only.
         */
        openFma120(
            appContext
        )


        /*
         * Continue automatic mode after USB permission.
         */
        if (autoModeEnabled) {

            startAutomaticScan()
        }
    }


    // =========================================================
    // OPEN FMA120
    // =========================================================

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


        /*
         * If the controller is already open,
         * do not close/reopen it unnecessarily.
         */
        if (
            controller != null &&
            controlChannelOpen
        ) {

            Log.i(
                TAG,
                "FMA120 controller already open"
            )


            return
        }


        val oldController =
            controller


        if (oldController != null) {

            try {

                oldController.close()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error closing previous FMA controller",
                    exception
                )
            }


            controller =
                null
        }


        Log.i(
            TAG,
            "Opening FMA120 control channel"
        )


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

                    mainHandler.post {

                        Log.e(
                            TAG,
                            "FMA120 error: $message"
                        )


                        statusMessage =
                            message
                    }
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


            Log.e(
                TAG,
                "Unable to open FMA120 control channel"
            )


            return
        }


        controller =
            newController


        controlChannelOpen =
            true


        statusMessage =
            "FMA120 control channel open."


        Log.i(
            TAG,
            "FMA120 control channel opened"
        )


        /*
         * IMPORTANT:
         *
         * Do NOT call startScan() here.
         *
         * startAutoMode(), permission handling,
         * or manual scan() will start scanning.
         *
         * This prevents duplicate BC:BI commands.
         */
    }


    // =========================================================
    // START AUTOMATIC MODE
    // =========================================================

    fun startAutoMode(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        applicationContext =
            appContext


        Log.i(
            TAG,
            "AUTO: startAutoMode called"
        )


        autoModeEnabled =
            true


        activeAutoSource =
            null


        pendingAutoSource =
            null


        currentReceiveState =
            null


        cancelAutoRescan()


        cancelAudioRetry()


        stopAudioRelayInternal()


        refresh(
            appContext
        )


        Log.i(
            TAG,
            "AUTO: usbConnected=$usbConnected " +
                    "usbPermission=$usbPermissionGranted"
        )


        // -----------------------------------------------------
        // FMA120 missing
        // -----------------------------------------------------

        if (!usbConnected) {

            statusMessage =
                "Connect the FMA120 receiver."


            Log.i(
                TAG,
                "AUTO: FMA120 not connected"
            )


            return
        }


        // -----------------------------------------------------
        // USB permission missing
        // -----------------------------------------------------

        if (!usbPermissionGranted) {

            statusMessage =
                "Requesting FMA120 USB permission."


            requestUsbPermission(
                appContext
            )


            return
        }


        // -----------------------------------------------------
        // Open FMA120 if necessary
        // -----------------------------------------------------

        if (
            controller == null ||
            !controlChannelOpen
        ) {

            openFma120(
                appContext
            )
        }


        if (
            controller == null ||
            !controlChannelOpen
        ) {

            statusMessage =
                "FMA120 could not be opened."


            Log.e(
                TAG,
                "AUTO: controller unavailable after open"
            )


            return
        }


        // -----------------------------------------------------
        // Start exactly ONE automatic scan
        // -----------------------------------------------------

        startAutomaticScan()
    }


    // =========================================================
    // START AUTOMATIC SCAN
    // =========================================================

    private fun startAutomaticScan() {

        if (!autoModeEnabled) {

            return
        }


        val currentController =
            controller


        if (currentController == null) {

            statusMessage =
                "FMA120 controller is unavailable."


            Log.e(
                TAG,
                "AUTO: cannot scan; controller is null"
            )


            return
        }


        /*
         * We are looking for a new source.
         */
        activeAutoSource =
            null


        pendingAutoSource =
            null


        currentReceiveState =
            null


        fmaSources =
            emptyList()


        Log.i(
            TAG,
            "AUTO: sending FMA scan command"
        )


        currentController.startScan()


        statusMessage =
            "Automatically searching for Auracast broadcasts."
    }


    // =========================================================
    // STOP AUTOMATIC MODE
    // =========================================================

    fun stopAutoMode() {

        Log.i(
            TAG,
            "AUTO: stopAutoMode called"
        )


        autoModeEnabled =
            false


        cancelAutoRescan()


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


        if (currentController != null) {


            try {

                currentController.stopReceiving()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error stopping FMA receive",
                    exception
                )
            }


            try {

                currentController.stopScan()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error stopping FMA scan",
                    exception
                )
            }
        }


        statusMessage =
            "Automatic mode stopped."
    }


    // =========================================================
    // MANUAL SCAN
    // =========================================================

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


    // =========================================================
    // MANUAL RECEIVE
    // =========================================================

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


        Log.i(
            TAG,
            "MANUAL: receive ${source.broadcastName} " +
                    "IDs=${source.broadcastIDs}"
        )


        currentController.receive(
            source
        )


        statusMessage =
            "Connecting to ${source.broadcastName}."
    }


    // =========================================================
    // STOP RECEIVING
    // =========================================================

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


        cancelAudioRetry()


        stopAudioRelayInternal()


        statusMessage =
            "Broadcast reception stopped."
    }


    // =========================================================
    // FMA120 FOUND A BROADCAST
    // =========================================================

    private fun handleBroadcastFound(
        source: FmaReceiverBroadcast
    ) {

        Log.i(
            TAG,
            "AUTO: broadcast found " +
                    "name=${source.broadcastName}, " +
                    "rssi=${source.rssi}, " +
                    "address=${source.address}, " +
                    "ids=${source.broadcastIDs}"
        )


        // -----------------------------------------------------
        // Update the list shown by the UI
        // -----------------------------------------------------

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


        // =====================================================
        // AUTOMATIC CONNECTION
        // =====================================================

        if (!autoModeEnabled) {

            Log.i(
                TAG,
                "AUTO: disabled; source only added to list"
            )


            return
        }


        /*
         * Already streaming from a source.
         */
        if (activeAutoSource != null) {

            Log.i(
                TAG,
                "AUTO: already connected; ignoring new source"
            )


            return
        }


        /*
         * receive(source) has already been sent.
         * Wait for the BA state response.
         */
        if (pendingAutoSource != null) {

            Log.i(
                TAG,
                "AUTO: connection already pending"
            )


            return
        }


        val currentController =
            controller


        if (currentController == null) {

            statusMessage =
                "FMA120 controller is unavailable."


            Log.e(
                TAG,
                "AUTO: cannot receive; controller null"
            )


            return
        }


        /*
         * Remember the exact FMA source.
         */
        pendingAutoSource =
            source


        currentReceiveState =
            null


        statusMessage =
            "Automatically connecting to ${source.broadcastName}."


        Log.i(
            TAG,
            "AUTO: sending receive command " +
                    "name=${source.broadcastName}, " +
                    "ids=${source.broadcastIDs}"
        )


        /*
         * IMPORTANT:
         *
         * Do NOT stopScan() here.
         *
         * Your working manual flow performs:
         *
         * BC:BI
         * find source
         * BC:BA=<broadcastIDs>
         *
         * without sending BC:BI=00 first.
         */
        currentController.receive(
            source
        )
    }


    // =========================================================
    // FMA120 RECEIVE STATE
    // =========================================================

    private fun handleReceiveState(
        receiveState: FmaReceiveState
    ) {

        currentReceiveState =
            receiveState


        Log.i(
            TAG,
            "AUTO: receive state " +
                    "sourceId=${receiveState.sourceId}, " +
                    "ids=${receiveState.broadcastIDs}, " +
                    "sync=${receiveState.syncState}, " +
                    "encryption=${receiveState.encryptionState}, " +
                    "bis=${receiveState.bisState}, " +
                    "streaming=${receiveState.isStreaming}"
        )


        // -----------------------------------------------------
        // Broadcast Code requested
        // -----------------------------------------------------

        if (
            receiveState.needsBroadcastCode
        ) {

            val currentController =
                controller


            if (currentController == null) {

                statusMessage =
                    "FMA120 controller is unavailable."


                return
            }


            Log.i(
                TAG,
                "AUTO: FMA requested Broadcast Code"
            )


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


        // -----------------------------------------------------
        // Synchronization failed
        // -----------------------------------------------------

        if (
            receiveState.syncFailed
        ) {

            Log.e(
                TAG,
                "AUTO: FMA synchronization failed"
            )


            if (autoModeEnabled) {


                activeAutoSource =
                    null


                pendingAutoSource =
                    null


                cancelAudioRetry()


                stopAudioRelayInternal()


                statusMessage =
                    "Auracast connection failed. Searching again."


                scheduleAutoRescan()


                return
            }


            statusMessage =
                "FMA120 synchronization failed."


            return
        }


        // -----------------------------------------------------
        // STREAMING
        // -----------------------------------------------------

        if (
            receiveState.isStreaming
        ) {

            Log.i(
                TAG,
                "AUTO: FMA120 STREAMING"
            )


            if (autoModeEnabled) {


                /*
                 * Connection succeeded.
                 */
                val pending =
                    pendingAutoSource


                if (pending != null) {

                    activeAutoSource =
                        pending
                }


                pendingAutoSource =
                    null


                val active =
                    activeAutoSource


                if (active != null) {

                    statusMessage =
                        "Automatically connected to ${active.broadcastName}."

                } else {

                    statusMessage =
                        "Auracast broadcast is streaming."
                }


                /*
                 * Start:
                 *
                 * FMA120 USB audio
                 *      ↓
                 * AudioRecord
                 *      ↓
                 * AudioTrack
                 *      ↓
                 * hearing/Bluetooth output
                 */
                if (!audioTestRunning) {

                    startAutomaticAudio()
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


    // =========================================================
    // AUTOMATIC RECONNECT
    // =========================================================

    private fun scheduleAutoRescan() {

        cancelAutoRescan()


        if (!autoModeEnabled) {

            return
        }


        val runnable =
            Runnable {


                autoRescanRunnable =
                    null


                if (!autoModeEnabled) {

                    return@Runnable
                }


                val currentController =
                    controller


                if (currentController == null) {

                    statusMessage =
                        "FMA120 controller is unavailable."


                    return@Runnable
                }


                Log.i(
                    TAG,
                    "AUTO: stopping failed receive session"
                )


                try {

                    currentController.stopReceiving()

                } catch (
                    exception: Exception
                ) {

                    Log.w(
                        TAG,
                        "AUTO: stopReceiving failed",
                        exception
                    )
                }


                activeAutoSource =
                    null


                pendingAutoSource =
                    null


                currentReceiveState =
                    null


                fmaSources =
                    emptyList()


                Log.i(
                    TAG,
                    "AUTO: restarting scan after failure"
                )


                currentController.startScan()


                statusMessage =
                    "Searching again for an Auracast transmitter."
            }


        autoRescanRunnable =
            runnable


        mainHandler.postDelayed(
            runnable,
            AUTO_RESCAN_DELAY_MS
        )
    }


    private fun cancelAutoRescan() {

        val runnable =
            autoRescanRunnable


        if (runnable != null) {

            mainHandler.removeCallbacks(
                runnable
            )
        }


        autoRescanRunnable =
            null
    }


    // =========================================================
    // AUTOMATIC AUDIO
    // =========================================================

    private fun startAutomaticAudio() {

        cancelAudioRetry()


        audioRetryCount =
            0


        Log.i(
            TAG,
            "AUTO: starting automatic audio relay"
        )


        attemptAutomaticAudio()
    }


    private fun attemptAutomaticAudio() {

        if (!autoModeEnabled) {

            return
        }


        /*
         * Do not create another relay
         * if one is already running.
         */
        if (audioTestRunning) {

            return
        }


        val context =
            applicationContext


        if (context == null) {

            statusMessage =
                "Application context is unavailable."


            return
        }


        Log.i(
            TAG,
            "AUTO: audio relay attempt " +
                    "${audioRetryCount + 1}"
        )


        startAudioTest(
            context
        )


        /*
         * Successful.
         */
        if (audioTestRunning) {

            Log.i(
                TAG,
                "AUTO: audio relay started"
            )


            return
        }


        /*
         * Android may need a short period before
         * the USB audio endpoint becomes available.
         */
        if (
            audioRetryCount >=
            MAX_AUDIO_RETRIES
        ) {

            Log.e(
                TAG,
                "AUTO: audio relay failed after retries"
            )


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


    // =========================================================
    // AUDIO RELAY
    // =========================================================

    fun startAudioTest(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        applicationContext =
            appContext


        refresh(
            appContext
        )


        // -----------------------------------------------------
        // RECORD_AUDIO permission
        // -----------------------------------------------------

        if (!recordAudioGranted) {

            statusMessage =
                "RECORD_AUDIO permission is required."


            Log.e(
                TAG,
                "Audio: RECORD_AUDIO permission missing"
            )


            return
        }


        val audioManager =
            appContext.getSystemService(
                AudioManager::class.java
            )


        if (audioManager == null) {

            statusMessage =
                "Audio service is unavailable."


            return
        }


        // =====================================================
        // FIND FMA120 USB AUDIO INPUT
        // =====================================================

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


            Log.i(
                TAG,
                "Audio: FMA120 USB input not found yet"
            )


            return
        }


        Log.i(
            TAG,
            "Audio: USB input " +
                    "id=${usbInput.id}, " +
                    "type=${usbInput.type}, " +
                    "name=${usbInput.productName}"
        )


        // =====================================================
        // FIND HEARING / BLUETOOTH OUTPUT
        // =====================================================

        var hearingOutput:
                AudioDeviceInfo? = null


        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )


        /*
         * Prefer real hearing-aid outputs.
         */
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


        /*
         * Fall back to Bluetooth headphones /
         * earbuds / BLE speakers.
         */
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


            Log.i(
                TAG,
                "Audio: Bluetooth/hearing output not found yet"
            )


            return
        }


        Log.i(
            TAG,
            "Audio: output " +
                    "id=${hearingOutput.id}, " +
                    "type=${hearingOutput.type}, " +
                    "name=${hearingOutput.productName}"
        )


        // =====================================================
        // STOP OLD RELAY
        // =====================================================

        stopAudioRelayInternal()


        // =====================================================
        // CREATE NEW RELAY
        // =====================================================

        val newRelay =
            UsbAudioRelay(
                context =
                    appContext,

                onError = {
                        message ->

                    mainHandler.post {


                        Log.e(
                            TAG,
                            "Audio relay error: $message"
                        )


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
                "Audio relay could not be started."


            Log.e(
                TAG,
                "Audio: UsbAudioRelay.start() returned false"
            )


            return
        }


        audioRelay =
            newRelay


        audioTestRunning =
            true


        // -----------------------------------------------------
        // Status
        // -----------------------------------------------------

        if (autoModeEnabled) {

            val active =
                activeAutoSource


            if (active != null) {

                statusMessage =
                    "Playing ${active.broadcastName} automatically."

            } else {

                statusMessage =
                    "Automatic Auracast audio relay started."
            }

        } else {

            statusMessage =
                "FMA120 USB audio relay started."
        }


        Log.i(
            TAG,
            "Audio: relay successfully started"
        )
    }


    // =========================================================
    // STOP AUDIO
    // =========================================================

    fun stopAudioTest() {

        cancelAudioRetry()


        stopAudioRelayInternal()


        statusMessage =
            "Audio test stopped."
    }


    private fun stopAudioRelayInternal() {

        val currentRelay =
            audioRelay


        if (currentRelay != null) {

            try {

                currentRelay.stop()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error stopping audio relay",
                    exception
                )
            }
        }


        audioRelay =
            null


        audioTestRunning =
            false
    }


    // =========================================================
    // CLOSE EVERYTHING
    // =========================================================

    fun close() {

        Log.i(
            TAG,
            "Closing FMA debug ViewModel resources"
        )


        autoModeEnabled =
            false


        cancelAutoRescan()


        cancelAudioRetry()


        stopAudioRelayInternal()


        val currentController =
            controller


        if (currentController != null) {


            try {

                currentController.stopReceiving()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error stopping receive",
                    exception
                )
            }


            try {

                currentController.stopScan()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error stopping scan",
                    exception
                )
            }


            try {

                currentController.close()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Error closing controller",
                    exception
                )
            }
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


    // =========================================================
    // HELPERS
    // =========================================================

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


    // =========================================================
    // VIEWMODEL DESTROYED
    // =========================================================

    override fun onCleared() {

        close()


        super.onCleared()
    }


    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {


        private const val TAG =
            "FmaDebugVM"


        const val ACTION_USB_PERMISSION =
            "com.innovatex.auracast.FMA_DEBUG_USB_PERMISSION"


        /*
         * This is only needed if the broadcast is encrypted.
         *
         * It must match the Broadcast Code used by
         * the transmitter.
         */
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