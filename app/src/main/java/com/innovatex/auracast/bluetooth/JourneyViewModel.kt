package com.innovatex.auracast.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.data.TransitRoute


class JourneyViewModel : ViewModel() {


    var state by mutableStateOf<JourneyState?>(
        null
    )
        private set


    private var fmaReceiver:
            Fma120ReceiverController? = null


    private var applicationContext:
            Context? = null


    private var audioRelay:
            UsbAudioRelay? = null


    private var connecting =
        false


    private var connected =
        false


    private var journeyActive =
        false


    private var pendingSource:
            FmaReceiverBroadcast? = null


    private var activeSource:
            FmaReceiverBroadcast? = null


    private var scanAttempt =
        0


    private var audioRetryCount =
        0


    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    /*
     * ---------------------------------------------------------
     * Scan watchdog
     *
     * If one FMA120 scan does not find anything,
     * restart the scan instead of waiting forever.
     * ---------------------------------------------------------
     */

    private val scanWatchdogRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (connected) {
                return@Runnable
            }

            if (connecting) {
                return@Runnable
            }


            Log.w(
                TAG,
                "SCAN: no transmitter found during scan window"
            )


            restartFmaScan()
        }


    /*
     * Used after stopping an old scan.
     */
    private val scanRestartRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (connected) {
                return@Runnable
            }

            if (connecting) {
                return@Runnable
            }


            startFmaSearch()
        }


    /*
     * Sends the receive command shortly after
     * stopping discovery.
     */
    private val receiveRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (!connecting) {
                return@Runnable
            }


            val receiver =
                fmaReceiver

            val source =
                pendingSource


            if (
                receiver == null ||
                source == null
            ) {

                Log.e(
                    TAG,
                    "CONNECT: receiver or pending source missing"
                )

                handleConnectionFailure(
                    "Receiver/source unavailable"
                )

                return@Runnable
            }


            Log.i(
                TAG,
                "CONNECT: sending receive command for " +
                        source.broadcastName
            )


            receiver.receive(
                source
            )


            /*
             * Do not remain in CONNECTING forever.
             */
            mainHandler.removeCallbacks(
                connectionTimeoutRunnable
            )

            mainHandler.postDelayed(
                connectionTimeoutRunnable,
                CONNECTION_TIMEOUT_MS
            )
        }


    /*
     * If FMA120 never reaches streaming,
     * abandon this connection and scan again.
     */
    private val connectionTimeoutRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (!connecting) {
                return@Runnable
            }

            if (connected) {
                return@Runnable
            }


            Log.e(
                TAG,
                "CONNECT: connection timed out"
            )


            handleConnectionFailure(
                "Connection timeout"
            )
        }


    /*
     * Restart searching after failed connection.
     */
    private val reconnectRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (connected) {
                return@Runnable
            }

            if (connecting) {
                return@Runnable
            }


            startFmaSearch()
        }


    /*
     * Retry audio device discovery because sometimes
     * Android exposes the USB/audio route slightly later
     * than the FMA receive-state callback.
     */
    private val audioRetryRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }

            if (!connected) {
                return@Runnable
            }

            if (audioRelay != null) {
                return@Runnable
            }


            startAutomaticAudio()
        }


    // =========================================================
    // START JOURNEY
    // =========================================================

    fun startJourney(
        context: Context,
        route: TransitRoute
    ) {

        Log.i(
            TAG,
            "AUTO: startJourney() called"
        )


        if (state != null) {

            Log.i(
                TAG,
                "AUTO: journey already running"
            )

            return
        }


        val appContext =
            context.applicationContext


        applicationContext =
            appContext


        journeyActive =
            true


        connecting =
            false


        connected =
            false


        pendingSource =
            null


        activeSource =
            null


        scanAttempt =
            0


        audioRetryCount =
            0


        state =
            JourneyState(
                route =
                    route,

                currentStopIndex =
                    0,

                phase =
                    JourneyPhase.SEARCHING,

                deviceAddress =
                    null,

                phaseStartedAt =
                    System.currentTimeMillis()
            )


        val receiver =
            Fma120ReceiverController(
                context =
                    appContext,

                onBroadcastFound = {
                        source ->

                    /*
                     * USB callbacks may come from another thread.
                     * Move state/UI work onto the main thread.
                     */
                    mainHandler.post {

                        onFmaBroadcastFound(
                            source
                        )
                    }
                },

                onReceiveStateChanged = {
                        receiveState ->

                    mainHandler.post {

                        onFmaReceiveStateChanged(
                            receiveState
                        )
                    }
                },

                onError = {
                        message ->

                    mainHandler.post {

                        Log.e(
                            TAG,
                            "FMA ERROR: $message"
                        )
                    }
                }
            )


        fmaReceiver =
            receiver


        Log.i(
            TAG,
            "AUTO: opening FMA120"
        )


        val opened =
            receiver.open()


        Log.i(
            TAG,
            "AUTO: FMA120 open result = $opened"
        )


        if (!opened) {

            Log.e(
                TAG,
                "AUTO: FMA120 could not be opened"
            )


            state =
                state?.copy(
                    phase =
                        JourneyPhase.DROP_OUT,

                    phaseStartedAt =
                        System.currentTimeMillis()
                )


            return
        }


        /*
         * Start reliable repeated scanning.
         */
        startFmaSearch()
    }


    // =========================================================
    // SCANNING
    // =========================================================

    private fun startFmaSearch() {

        if (!journeyActive) {
            return
        }


        if (connected) {
            return
        }


        if (connecting) {
            return
        }


        val receiver =
            fmaReceiver


        if (receiver == null) {

            Log.e(
                TAG,
                "SCAN: FMA120 receiver is null"
            )

            return
        }


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            scanRestartRunnable
        )


        scanAttempt++


        Log.i(
            TAG,
            "SCAN: starting scan attempt $scanAttempt"
        )


        state =
            state?.copy(
                deviceAddress =
                    null,

                phase =
                    JourneyPhase.SEARCHING,

                phaseStartedAt =
                    System.currentTimeMillis()
            )


        /*
         * Sends BC:BI
         */
        receiver.startScan()


        /*
         * If this scan doesn't find anything,
         * stop it and start another clean scan.
         */
        mainHandler.postDelayed(
            scanWatchdogRunnable,
            SCAN_WINDOW_MS
        )
    }


    private fun restartFmaScan() {

        if (!journeyActive) {
            return
        }


        if (connected) {
            return
        }


        if (connecting) {
            return
        }


        val receiver =
            fmaReceiver


        if (receiver == null) {
            return
        }


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        Log.i(
            TAG,
            "SCAN: restarting FMA120 scan"
        )


        /*
         * Sends BC:BI=00
         */
        receiver.stopScan()


        /*
         * Small pause between stop and next scan.
         */
        mainHandler.removeCallbacks(
            scanRestartRunnable
        )


        mainHandler.postDelayed(
            scanRestartRunnable,
            SCAN_RESTART_DELAY_MS
        )
    }


    // =========================================================
    // TRANSMITTER FOUND
    // =========================================================

    private fun onFmaBroadcastFound(
        source: FmaReceiverBroadcast
    ) {

        if (!journeyActive) {
            return
        }


        Log.i(
            TAG,
            "SCAN: TRANSMITTER FOUND"
        )


        Log.i(
            TAG,
            "SCAN: name = ${source.broadcastName}"
        )


        Log.i(
            TAG,
            "SCAN: address = ${source.address}"
        )


        Log.i(
            TAG,
            "SCAN: RSSI = ${source.rssi}"
        )


        Log.i(
            TAG,
            "SCAN: broadcastIDs = ${source.broadcastIDs}"
        )


        /*
         * First transmitter wins.
         *
         * No GPS.
         * No route matching.
         * No stop matching.
         */
        if (connected) {

            Log.i(
                TAG,
                "SCAN: already connected - ignoring source"
            )

            return
        }


        if (connecting) {

            Log.i(
                TAG,
                "SCAN: connection already in progress"
            )

            return
        }


        val receiver =
            fmaReceiver


        if (receiver == null) {

            Log.e(
                TAG,
                "SCAN: FMA receiver is null"
            )

            return
        }


        /*
         * We found a transmitter.
         * Cancel scan retry.
         */
        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            scanRestartRunnable
        )


        connecting =
            true


        pendingSource =
            source


        state =
            state?.copy(
                deviceAddress =
                    source.address,

                phase =
                    JourneyPhase.CONNECTING,

                phaseStartedAt =
                    System.currentTimeMillis()
            )


        Log.i(
            TAG,
            "CONNECT: automatically connecting to " +
                    source.broadcastName
        )


        /*
         * Stop FMA discovery before asking it to
         * receive the selected broadcast.
         */
        receiver.stopScan()


        /*
         * Short application-side delay.
         *
         * This avoids immediately sending another
         * command after stopScan().
         */
        mainHandler.removeCallbacks(
            receiveRunnable
        )


        mainHandler.postDelayed(
            receiveRunnable,
            RECEIVE_AFTER_SCAN_DELAY_MS
        )
    }


    // =========================================================
    // FMA RECEIVE STATE
    // =========================================================

    private fun onFmaReceiveStateChanged(
        receiveState: FmaReceiveState
    ) {

        if (!journeyActive) {
            return
        }


        Log.i(
            TAG,
            "RECEIVE: sourceId = ${receiveState.sourceId}, " +
                    "sync=${receiveState.syncState}, " +
                    "encryption=${receiveState.encryptionState}, " +
                    "BIS=${receiveState.bisState}"
        )


        // -----------------------------------------------------
        // Encrypted broadcast
        // -----------------------------------------------------

        if (
            receiveState.needsBroadcastCode
        ) {

            val receiver =
                fmaReceiver


            if (receiver != null) {

                Log.i(
                    TAG,
                    "RECEIVE: sending Broadcast Code"
                )


                receiver.provideBroadcastCode(
                    sourceId =
                        receiveState.sourceId,

                    code =
                        BROADCAST_CODE
                )
            }


            return
        }


        // -----------------------------------------------------
        // Synchronisation failed
        // -----------------------------------------------------

        if (
            receiveState.syncFailed
        ) {

            Log.e(
                TAG,
                "RECEIVE: synchronization failed"
            )


            handleConnectionFailure(
                "Synchronization failed"
            )


            return
        }


        // -----------------------------------------------------
        // Streaming success
        // -----------------------------------------------------

        if (
            receiveState.isStreaming
        ) {

            Log.i(
                TAG,
                "RECEIVE: SUCCESS - FMA120 STREAMING"
            )


            mainHandler.removeCallbacks(
                connectionTimeoutRunnable
            )


            mainHandler.removeCallbacks(
                receiveRunnable
            )


            connecting =
                false


            connected =
                true


            activeSource =
                pendingSource


            pendingSource =
                null


            state =
                state?.copy(
                    phase =
                        JourneyPhase.RECEIVING,

                    phaseStartedAt =
                        System.currentTimeMillis()
                )


            /*
             * FMA120 is now receiving Auracast.
             *
             * Start:
             *
             * FMA120 USB audio
             *       ↓
             * Android
             *       ↓
             * headphones / hearing device
             */
            if (audioRelay == null) {

                Log.i(
                    TAG,
                    "AUDIO: starting automatic audio"
                )


                startAutomaticAudio()
            }


            return
        }
    }


    // =========================================================
    // CONNECTION FAILURE / RETRY
    // =========================================================

    private fun handleConnectionFailure(
        reason: String
    ) {

        Log.e(
            TAG,
            "CONNECT: connection failed - $reason"
        )


        mainHandler.removeCallbacks(
            connectionTimeoutRunnable
        )


        mainHandler.removeCallbacks(
            receiveRunnable
        )


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        stopAutomaticAudio()


        connecting =
            false


        connected =
            false


        pendingSource =
            null


        activeSource =
            null


        val receiver =
            fmaReceiver


        if (receiver != null) {

            /*
             * Sends BC:BA=00
             */
            receiver.stopReceiving()
        }


        state =
            state?.copy(
                deviceAddress =
                    null,

                phase =
                    JourneyPhase.DROP_OUT,

                phaseStartedAt =
                    System.currentTimeMillis()
            )


        /*
         * Automatically return to searching.
         */
        mainHandler.removeCallbacks(
            reconnectRunnable
        )


        mainHandler.postDelayed(
            reconnectRunnable,
            CONNECTION_RETRY_DELAY_MS
        )
    }


    // =========================================================
    // AUTOMATIC AUDIO
    // =========================================================

    private fun startAutomaticAudio() {

        if (!journeyActive) {
            return
        }


        if (!connected) {

            Log.i(
                TAG,
                "AUDIO: FMA120 is not streaming yet"
            )

            return
        }


        if (audioRelay != null) {

            Log.i(
                TAG,
                "AUDIO: relay already running"
            )

            return
        }


        val context =
            applicationContext


        if (context == null) {

            Log.e(
                TAG,
                "AUDIO: application context unavailable"
            )

            return
        }


        // -----------------------------------------------------
        // RECORD_AUDIO permission
        // -----------------------------------------------------

        val permission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            )


        if (
            permission !=
            PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                TAG,
                "AUDIO: RECORD_AUDIO permission not granted"
            )

            return
        }


        val audioManager =
            context.getSystemService(
                AudioManager::class.java
            )


        if (audioManager == null) {

            Log.e(
                TAG,
                "AUDIO: AudioManager unavailable"
            )

            return
        }


        // =====================================================
        // Find FMA120 USB input
        // =====================================================

        val inputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_INPUTS
            )


        var usbInput:
                AudioDeviceInfo? = null


        /*
         * First try to identify the actual FMA120
         * by its product name.
         */
        for (device in inputs) {

            Log.i(
                TAG,
                "AUDIO INPUT: " +
                        "id=${device.id}, " +
                        "type=${device.type}, " +
                        "name=${device.productName}"
            )


            val name =
                device.productName
                    .toString()
                    .uppercase()


            val isUsb =
                device.type ==
                        AudioDeviceInfo.TYPE_USB_DEVICE ||
                        device.type ==
                        AudioDeviceInfo.TYPE_USB_HEADSET


            if (
                isUsb &&
                (
                        name.contains("FMA120") ||
                                name.contains("FLOOGOO")
                        )
            ) {

                usbInput =
                    device

                break
            }
        }


        /*
         * Fallback:
         * use the first USB audio input.
         */
        if (usbInput == null) {

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
        }


        if (usbInput == null) {

            scheduleAudioRetry(
                "FMA120 USB audio input not found"
            )

            return
        }


        Log.i(
            TAG,
            "AUDIO: FMA120 USB INPUT FOUND: " +
                    "${usbInput.productName}, " +
                    "type=${usbInput.type}"
        )


        // =====================================================
        // Find headphones / hearing aid
        // =====================================================

        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )


        var hearingOutput:
                AudioDeviceInfo? = null


        /*
         * Prefer actual hearing aid output.
         */
        for (device in outputs) {

            Log.i(
                TAG,
                "AUDIO OUTPUT: " +
                        "id=${device.id}, " +
                        "type=${device.type}, " +
                        "name=${device.productName}"
            )


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
         * If no hearing aid is connected,
         * use normal Bluetooth headphones/speaker.
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

            scheduleAudioRetry(
                "Bluetooth/hearing output not found"
            )

            return
        }


        Log.i(
            TAG,
            "AUDIO: OUTPUT FOUND: " +
                    "${hearingOutput.productName}, " +
                    "type=${hearingOutput.type}"
        )


        // =====================================================
        // Start FMA120 → Android → headphones relay
        // =====================================================

        val relay =
            UsbAudioRelay(
                context =
                    context,

                onError = {
                        message ->

                    mainHandler.post {

                        Log.e(
                            TAG,
                            "AUDIO RELAY ERROR: $message"
                        )


                        val currentRelay =
                            audioRelay


                        if (currentRelay != null) {

                            currentRelay.stop()
                        }


                        audioRelay =
                            null


                        if (
                            journeyActive &&
                            connected
                        ) {

                            scheduleAudioRetry(
                                "Audio relay error"
                            )
                        }
                    }
                }
            )


        val started =
            relay.start(
                usbInput =
                    usbInput,

                hearingOutput =
                    hearingOutput
            )


        if (!started) {

            Log.e(
                TAG,
                "AUDIO: UsbAudioRelay.start() returned false"
            )


            relay.stop()


            scheduleAudioRetry(
                "Audio relay could not start"
            )


            return
        }


        audioRelay =
            relay


        audioRetryCount =
            0


        mainHandler.removeCallbacks(
            audioRetryRunnable
        )


        Log.i(
            TAG,
            "AUDIO: SUCCESS - AUDIO RELAY STARTED"
        )


        Log.i(
            TAG,
            "AUDIO: ${usbInput.productName} -> " +
                    "${hearingOutput.productName}"
        )
    }


    private fun scheduleAudioRetry(
        reason: String
    ) {

        if (!journeyActive) {
            return
        }


        if (!connected) {
            return
        }


        if (audioRelay != null) {
            return
        }


        if (
            audioRetryCount >=
            MAX_AUDIO_RETRIES
        ) {

            Log.e(
                TAG,
                "AUDIO: giving up after " +
                        "$MAX_AUDIO_RETRIES retries. " +
                        "Reason: $reason"
            )

            return
        }


        audioRetryCount++


        Log.w(
            TAG,
            "AUDIO: $reason. " +
                    "Retry $audioRetryCount/" +
                    "$MAX_AUDIO_RETRIES"
        )


        mainHandler.removeCallbacks(
            audioRetryRunnable
        )


        mainHandler.postDelayed(
            audioRetryRunnable,
            AUDIO_RETRY_DELAY_MS
        )
    }


    private fun stopAutomaticAudio() {

        mainHandler.removeCallbacks(
            audioRetryRunnable
        )


        audioRetryCount =
            0


        val relay =
            audioRelay


        if (relay != null) {

            Log.i(
                TAG,
                "AUDIO: stopping audio relay"
            )


            relay.stop()
        }


        audioRelay =
            null
    }


    // =========================================================
    // END JOURNEY
    // =========================================================

    fun endJourney() {

        Log.i(
            TAG,
            "AUTO: ending journey"
        )


        journeyActive =
            false


        /*
         * Cancel every pending automatic operation.
         */
        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            scanRestartRunnable
        )


        mainHandler.removeCallbacks(
            receiveRunnable
        )


        mainHandler.removeCallbacks(
            connectionTimeoutRunnable
        )


        mainHandler.removeCallbacks(
            reconnectRunnable
        )


        mainHandler.removeCallbacks(
            audioRetryRunnable
        )


        stopAutomaticAudio()


        val receiver =
            fmaReceiver


        if (receiver != null) {

            receiver.stopReceiving()

            receiver.stopScan()

            receiver.close()
        }


        fmaReceiver =
            null


        applicationContext =
            null


        pendingSource =
            null


        activeSource =
            null


        connecting =
            false


        connected =
            false


        scanAttempt =
            0


        audioRetryCount =
            0


        state =
            null
    }


    override fun onCleared() {

        endJourney()

        super.onCleared()
    }


    companion object {

        private const val TAG =
            "JourneyViewModel"


        private const val BROADCAST_CODE =
            "AURA86DEMO2026"


        /*
         * One scan attempt can run for 3 seconds.
         * If no transmitter is reported, restart it.
         */
        private const val SCAN_WINDOW_MS =
            3_000L


        /*
         * Pause between stopScan() and the next scan.
         */
        private const val SCAN_RESTART_DELAY_MS =
            300L


        /*
         * Pause between stopping discovery and
         * sending receive(source).
         */
        private const val RECEIVE_AFTER_SCAN_DELAY_MS =
            250L


        /*
         * Maximum time allowed for FMA120 to
         * reach streaming after receive(source).
         */
        private const val CONNECTION_TIMEOUT_MS =
            10_000L


        /*
         * Wait before searching again after a
         * connection/synchronisation failure.
         */
        private const val CONNECTION_RETRY_DELAY_MS =
            1_000L


        /*
         * Audio device discovery retry.
         */
        private const val AUDIO_RETRY_DELAY_MS =
            500L


        private const val MAX_AUDIO_RETRIES =
            10
    }
}