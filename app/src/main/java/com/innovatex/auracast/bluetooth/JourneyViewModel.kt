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


    // =========================================================
    // FMA120
    // =========================================================

    private var fmaReceiver:
            Fma120ReceiverController? = null


    private var applicationContext:
            Context? = null


    // =========================================================
    // AUDIO
    // =========================================================

    private var audioRelay:
            UsbAudioRelay? = null


    private var audioRetryCount =
        0


    // =========================================================
    // JOURNEY STATE
    // =========================================================

    private var journeyActive =
        false


    private var connecting =
        false


    private var connected =
        false


    // =========================================================
    // FMA SOURCES
    // =========================================================

    /*
     * All transmitters found during the current scan window.
     *
     * Key:
     * broadcastIDs
     */
    private val discoveredFmaSources =
        mutableMapOf<String, FmaReceiverBroadcast>()


    /*
     * Transmitters already successfully used.
     *
     * This prevents:
     *
     * A -> B -> A -> B
     *
     * Later, when you give me the four unique IDs,
     * we can replace this with an exact ordered list.
     */
    private val visitedFmaBroadcastIds =
        mutableSetOf<String>()


    /*
     * Temporarily ignore transmitters that repeatedly
     * fail to connect.
     */
    private val ignoredUntil =
        mutableMapOf<String, Long>()


    private var pendingFmaSource:
            FmaReceiverBroadcast? = null


    private var activeFmaSource:
            FmaReceiverBroadcast? = null


    // =========================================================
    // SCAN / CONNECTION CONTROL
    // =========================================================

    private var scanAttempt =
        0


    private var connectionAttempt =
        0


    private var selectionScheduled =
        false


    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    // =========================================================
    // SELECT STRONGEST TRANSMITTER
    // =========================================================

    private val selectStrongestRunnable =
        Runnable {

            selectionScheduled =
                false


            if (!journeyActive) {
                return@Runnable
            }


            if (connected) {
                return@Runnable
            }


            if (connecting) {
                return@Runnable
            }


            val now =
                System.currentTimeMillis()


            /*
             * Remove expired temporary ignores.
             */
            ignoredUntil.entries.removeAll {
                    entry ->

                entry.value <= now
            }


            var strongest:
                    FmaReceiverBroadcast? = null


            for (
            source in
            discoveredFmaSources.values
            ) {

                /*
                 * Do not go back to a transmitter
                 * already used earlier in the journey.
                 */
                if (
                    visitedFmaBroadcastIds.contains(
                        source.broadcastIDs
                    )
                ) {

                    Log.i(
                        TAG,
                        "SCAN: ignoring visited transmitter " +
                                source.broadcastName
                    )

                    continue
                }


                /*
                 * Ignore temporarily failed source.
                 */
                if (
                    ignoredUntil.containsKey(
                        source.broadcastIDs
                    )
                ) {

                    Log.i(
                        TAG,
                        "SCAN: temporarily ignoring " +
                                source.broadcastName
                    )

                    continue
                }


                if (strongest == null) {

                    strongest =
                        source

                    continue
                }


                /*
                 * RSSI:
                 *
                 * -45 is stronger than -70.
                 *
                 * Therefore the greater RSSI number wins.
                 */
                if (
                    source.rssi >
                    strongest.rssi
                ) {

                    strongest =
                        source
                }
            }


            if (strongest == null) {

                Log.i(
                    TAG,
                    "SCAN: no usable transmitter found"
                )


                restartDirectFmaScan()


                return@Runnable
            }


            Log.i(
                TAG,
                "SCAN: strongest transmitter selected"
            )


            Log.i(
                TAG,
                "SCAN: name=${strongest.broadcastName}"
            )


            Log.i(
                TAG,
                "SCAN: RSSI=${strongest.rssi}"
            )


            Log.i(
                TAG,
                "SCAN: ID=${strongest.broadcastIDs}"
            )


            connectDirectlyToFmaSource(
                strongest
            )
        }


    // =========================================================
    // SCAN WATCHDOG
    // =========================================================

    /*
     * If FMA120 does not report any useful transmitter
     * during the scan window, restart scanning.
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


            if (
                discoveredFmaSources.isNotEmpty()
            ) {

                /*
                 * We already received some results.
                 * Selection should handle them.
                 */
                return@Runnable
            }


            Log.w(
                TAG,
                "SCAN: no transmitter detected - restarting"
            )


            restartDirectFmaScan()
        }


    // =========================================================
    // CONNECTION TIMEOUT
    // =========================================================

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
                "CONNECT: timeout"
            )


            retryPendingConnection(
                "Connection timeout"
            )
        }


    // =========================================================
    // STREAM LOSS
    // =========================================================

    /*
     * FMA may briefly report a non-streaming state.
     *
     * We wait before deciding the transmitter is really lost.
     */
    private val streamLossRunnable =
        Runnable {

            if (!journeyActive) {
                return@Runnable
            }


            if (!connected) {
                return@Runnable
            }


            Log.w(
                TAG,
                "HANDOVER: current stream lost"
            )


            handleActiveSourceLost()
        }


    // =========================================================
    // AUDIO RETRY
    // =========================================================

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
            "JOURNEY: startJourney()"
        )


        if (state != null) {

            Log.i(
                TAG,
                "JOURNEY: already running"
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


        scanAttempt =
            0


        connectionAttempt =
            0


        audioRetryCount =
            0


        selectionScheduled =
            false


        pendingFmaSource =
            null


        activeFmaSource =
            null


        discoveredFmaSources.clear()


        visitedFmaBroadcastIds.clear()


        ignoredUntil.clear()


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


        // =====================================================
        // CREATE FMA120 CONTROLLER
        // =====================================================

        val receiver =
            Fma120ReceiverController(
                context =
                    appContext,

                onBroadcastFound = {
                        source ->

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


        // =====================================================
        // OPEN FMA120
        // =====================================================

        Log.i(
            TAG,
            "FMA: opening FMA120"
        )


        val opened =
            receiver.open()


        Log.i(
            TAG,
            "FMA: open result=$opened"
        )


        if (!opened) {

            Log.e(
                TAG,
                "FMA: unable to open receiver"
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
         * IMPORTANT:
         *
         * Start FMA120 scanning immediately.
         *
         * We do NOT wait for Android BroadcastScanner.
         *
         * This matches the scanning method that already
         * worked successfully in your test.
         */
        Log.i(
            TAG,
            "SCAN: starting FMA120 discovery immediately"
        )


        startDirectFmaScan()
    }


    // =========================================================
    // FMA120 SCAN
    // =========================================================

    private fun startDirectFmaScan() {

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
                "SCAN: FMA receiver unavailable"
            )

            return
        }


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            selectStrongestRunnable
        )


        selectionScheduled =
            false


        discoveredFmaSources.clear()


        scanAttempt++


        Log.i(
            TAG,
            "SCAN: attempt $scanAttempt"
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
         * Sends:
         *
         * BC:BI
         */
        receiver.startScan()


        /*
         * Restart if the FMA120 returns nothing.
         */
        mainHandler.postDelayed(
            scanWatchdogRunnable,
            FMA_SCAN_WINDOW_MS
        )
    }


    // =========================================================
    // RESTART SCAN
    // =========================================================

    private fun restartDirectFmaScan() {

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


        mainHandler.removeCallbacks(
            selectStrongestRunnable
        )


        selectionScheduled =
            false


        discoveredFmaSources.clear()


        Log.i(
            TAG,
            "SCAN: stopping old scan"
        )


        /*
         * Sends:
         *
         * BC:BI=00
         */
        receiver.stopScan()


        mainHandler.postDelayed(
            {

                if (
                    journeyActive &&
                    !connected &&
                    !connecting
                ) {

                    startDirectFmaScan()
                }

            },
            FMA_SCAN_RESTART_DELAY_MS
        )
    }


    // =========================================================
    // FMA BROADCAST FOUND
    // =========================================================

    private fun onFmaBroadcastFound(
        source: FmaReceiverBroadcast
    ) {

        if (!journeyActive) {
            return
        }


        Log.i(
            TAG,
            "FMA FOUND:"
        )


        Log.i(
            TAG,
            "FMA FOUND: name=${source.broadcastName}"
        )


        Log.i(
            TAG,
            "FMA FOUND: address=${source.address}"
        )


        Log.i(
            TAG,
            "FMA FOUND: RSSI=${source.rssi}"
        )


        Log.i(
            TAG,
            "FMA FOUND: ID=${source.broadcastIDs}"
        )


        /*
         * While receiving, don't start another FMA connection.
         *
         * The next scan starts when the current stream is lost.
         */
        if (connected) {
            return
        }


        if (connecting) {
            return
        }


        /*
         * Previous transmitter?
         */
        if (
            visitedFmaBroadcastIds.contains(
                source.broadcastIDs
            )
        ) {

            Log.i(
                TAG,
                "FMA FOUND: already visited - ignoring"
            )

            return
        }


        /*
         * Failed recently?
         */
        val ignoredUntilTime =
            ignoredUntil[
                source.broadcastIDs
            ]


        if (
            ignoredUntilTime != null &&
            ignoredUntilTime >
            System.currentTimeMillis()
        ) {

            Log.i(
                TAG,
                "FMA FOUND: temporarily ignored"
            )

            return
        }


        /*
         * Store/update transmitter.
         *
         * If FMA reports it again with a new RSSI,
         * the latest value replaces the old one.
         */
        discoveredFmaSources[
            source.broadcastIDs
        ] =
            source


        /*
         * Give FMA120 a short period to report all nearby
         * transmitters before choosing the strongest.
         *
         * We DON'T immediately connect to the first packet.
         */
        if (!selectionScheduled) {

            selectionScheduled =
                true


            mainHandler.removeCallbacks(
                selectStrongestRunnable
            )


            mainHandler.postDelayed(
                selectStrongestRunnable,
                SOURCE_SELECTION_WINDOW_MS
            )
        }
    }


    // =========================================================
    // CONNECT
    // =========================================================

    private fun connectDirectlyToFmaSource(
        source: FmaReceiverBroadcast
    ) {

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
                "CONNECT: receiver unavailable"
            )

            return
        }


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            selectStrongestRunnable
        )


        selectionScheduled =
            false


        connecting =
            true


        connected =
            false


        connectionAttempt =
            0


        pendingFmaSource =
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
            "CONNECT: selected ${source.broadcastName}"
        )


        Log.i(
            TAG,
            "CONNECT: RSSI=${source.rssi}"
        )


        Log.i(
            TAG,
            "CONNECT: ID=${source.broadcastIDs}"
        )


        /*
         * Stop scanning before receiving.
         */
        receiver.stopScan()


        sendReceiveCommandAfterDelay(
            source
        )
    }


    // =========================================================
    // SEND RECEIVE COMMAND
    // =========================================================

    private fun sendReceiveCommandAfterDelay(
        source: FmaReceiverBroadcast
    ) {

        mainHandler.postDelayed(
            {

                if (!journeyActive) {
                    return@postDelayed
                }


                if (!connecting) {
                    return@postDelayed
                }


                if (connected) {
                    return@postDelayed
                }


                val receiver =
                    fmaReceiver


                if (receiver == null) {
                    return@postDelayed
                }


                connectionAttempt++


                Log.i(
                    TAG,
                    "CONNECT: receive attempt " +
                            "$connectionAttempt/" +
                            "$MAX_CONNECTION_ATTEMPTS"
                )


                Log.i(
                    TAG,
                    "CONNECT: sending receive command"
                )


                /*
                 * Sends:
                 *
                 * BC:BA=<broadcastIDs>
                 */
                receiver.receive(
                    source
                )


                mainHandler.removeCallbacks(
                    connectionTimeoutRunnable
                )


                mainHandler.postDelayed(
                    connectionTimeoutRunnable,
                    CONNECTION_TIMEOUT_MS
                )

            },
            RECEIVE_AFTER_SCAN_DELAY_MS
        )
    }


    // =========================================================
    // RECEIVE STATE
    // =========================================================

    private fun onFmaReceiveStateChanged(
        receiveState: FmaReceiveState
    ) {

        if (!journeyActive) {
            return
        }


        Log.i(
            TAG,
            "FMA STATE: " +
                    "source=${receiveState.sourceId}, " +
                    "sync=${receiveState.syncState}, " +
                    "encryption=${receiveState.encryptionState}, " +
                    "BIS=${receiveState.bisState}"
        )


        // =====================================================
        // BROADCAST CODE REQUIRED
        // =====================================================

        if (
            receiveState.needsBroadcastCode
        ) {

            val receiver =
                fmaReceiver


            if (receiver != null) {

                Log.i(
                    TAG,
                    "FMA: sending Broadcast Code"
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


        // =====================================================
        // SYNC FAILED
        // =====================================================

        if (
            receiveState.syncFailed
        ) {

            Log.e(
                TAG,
                "FMA: synchronization failed"
            )


            if (connected) {

                handleActiveSourceLost()

            } else {

                retryPendingConnection(
                    "Synchronization failed"
                )
            }


            return
        }


        // =====================================================
        // STREAMING
        // =====================================================

        if (
            receiveState.isStreaming
        ) {

            Log.i(
                TAG,
                "FMA: SUCCESS - STREAMING"
            )


            /*
             * Cancel any pending stream-loss decision.
             */
            mainHandler.removeCallbacks(
                streamLossRunnable
            )


            mainHandler.removeCallbacks(
                connectionTimeoutRunnable
            )


            val source =
                pendingFmaSource


            if (source != null) {

                activeFmaSource =
                    source


                /*
                 * Remember this transmitter.
                 *
                 * When searching later, it will be ignored.
                 */
                visitedFmaBroadcastIds.add(
                    source.broadcastIDs
                )


                Log.i(
                    TAG,
                    "CONNECTED: ${source.broadcastName}"
                )


                Log.i(
                    TAG,
                    "CONNECTED: ID=${source.broadcastIDs}"
                )
            }


            pendingFmaSource =
                null


            connecting =
                false


            connected =
                true


            connectionAttempt =
                0


            scanAttempt =
                0


            discoveredFmaSources.clear()


            /*
             * Update UI stop index.
             *
             * First transmitter:
             * currentStopIndex = 0
             *
             * Second:
             * currentStopIndex = 1
             *
             * etc.
             */
            val current =
                state


            if (current != null) {

                var newStopIndex =
                    visitedFmaBroadcastIds.size -
                            1


                if (
                    current.route.stops.isNotEmpty()
                ) {

                    newStopIndex =
                        newStopIndex.coerceAtMost(
                            current.route.stops.lastIndex
                        )
                }


                newStopIndex =
                    newStopIndex.coerceAtLeast(
                        0
                    )


                state =
                    current.copy(
                        currentStopIndex =
                            newStopIndex,

                        phase =
                            JourneyPhase.RECEIVING,

                        phaseStartedAt =
                            System.currentTimeMillis()
                    )
            }


            /*
             * FMA120
             *      ↓ USB
             * Android
             *      ↓
             * headphones / hearing device
             */
            startAutomaticAudio()


            return
        }


        // =====================================================
        // STREAM STOPPED
        // =====================================================

        /*
         * If we were successfully connected and FMA now
         * reports a non-streaming state, wait briefly.
         *
         * If streaming returns, the timer is cancelled above.
         *
         * Otherwise move to next transmitter.
         */
        if (connected) {

            Log.w(
                TAG,
                "FMA: streaming state lost - waiting for confirmation"
            )


            mainHandler.removeCallbacks(
                streamLossRunnable
            )


            mainHandler.postDelayed(
                streamLossRunnable,
                STREAM_LOSS_CONFIRM_MS
            )
        }
    }


    // =========================================================
    // CONNECTION RETRY
    // =========================================================

    private fun retryPendingConnection(
        reason: String
    ) {

        Log.w(
            TAG,
            "CONNECT: failed - $reason"
        )


        mainHandler.removeCallbacks(
            connectionTimeoutRunnable
        )


        val source =
            pendingFmaSource


        if (source == null) {

            connecting =
                false


            state =
                state?.copy(
                    deviceAddress =
                        null,

                    phase =
                        JourneyPhase.SEARCHING,

                    phaseStartedAt =
                        System.currentTimeMillis()
                )


            restartDirectFmaScan()


            return
        }


        // =====================================================
        // RETRY SAME SOURCE
        // =====================================================

        if (
            connectionAttempt <
            MAX_CONNECTION_ATTEMPTS
        ) {

            Log.i(
                TAG,
                "CONNECT: retrying ${source.broadcastName}"
            )


            val receiver =
                fmaReceiver


            if (receiver != null) {

                receiver.stopReceiving()
            }


            mainHandler.postDelayed(
                {

                    if (
                        journeyActive &&
                        connecting &&
                        !connected
                    ) {

                        sendReceiveCommandAfterDelay(
                            source
                        )
                    }

                },
                CONNECTION_RETRY_DELAY_MS
            )


            return
        }


        // =====================================================
        // GIVE UP TEMPORARILY
        // =====================================================

        Log.e(
            TAG,
            "CONNECT: giving up temporarily on " +
                    source.broadcastName
        )


        ignoredUntil[
            source.broadcastIDs
        ] =
            System.currentTimeMillis() +
                    FAILED_SOURCE_COOLDOWN_MS


        val receiver =
            fmaReceiver


        if (receiver != null) {

            receiver.stopReceiving()

            receiver.stopScan()
        }


        pendingFmaSource =
            null


        connecting =
            false


        connected =
            false


        connectionAttempt =
            0


        state =
            state?.copy(
                deviceAddress =
                    null,

                phase =
                    JourneyPhase.SEARCHING,

                phaseStartedAt =
                    System.currentTimeMillis()
            )


        mainHandler.postDelayed(
            {

                if (
                    journeyActive &&
                    !connected &&
                    !connecting
                ) {

                    startDirectFmaScan()
                }

            },
            CONNECTION_RETRY_DELAY_MS
        )
    }


    // =========================================================
    // ACTIVE SOURCE LOST
    // =========================================================

    private fun handleActiveSourceLost() {

        if (!connected) {
            return
        }


        Log.i(
            TAG,
            "HANDOVER: leaving current transmitter"
        )


        val previous =
            activeFmaSource


        if (previous != null) {

            Log.i(
                TAG,
                "HANDOVER: previous=${previous.broadcastName}"
            )
        }


        mainHandler.removeCallbacks(
            streamLossRunnable
        )


        mainHandler.removeCallbacks(
            connectionTimeoutRunnable
        )


        stopAutomaticAudio()


        val receiver =
            fmaReceiver


        if (receiver != null) {

            /*
             * Stop current Auracast source.
             */
            receiver.stopReceiving()


            /*
             * Ensure previous scan is stopped.
             */
            receiver.stopScan()
        }


        connected =
            false


        connecting =
            false


        activeFmaSource =
            null


        pendingFmaSource =
            null


        connectionAttempt =
            0


        discoveredFmaSources.clear()


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
         * Search for another transmitter.
         *
         * The previous broadcast ID is already in
         * visitedFmaBroadcastIds, so it will be ignored.
         */
        mainHandler.postDelayed(
            {

                if (
                    journeyActive &&
                    !connected &&
                    !connecting
                ) {

                    Log.i(
                        TAG,
                        "HANDOVER: searching for next transmitter"
                    )


                    startDirectFmaScan()
                }

            },
            NEXT_SOURCE_SEARCH_DELAY_MS
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
                "AUDIO: context unavailable"
            )

            return
        }


        // =====================================================
        // RECORD_AUDIO PERMISSION
        // =====================================================

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
                "AUDIO: RECORD_AUDIO permission missing"
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
        // FIND FMA120 USB INPUT
        // =====================================================

        val inputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_INPUTS
            )


        var usbInput:
                AudioDeviceInfo? = null


        /*
         * Prefer device that actually identifies as
         * FMA120 / FlooGoo.
         */
        for (device in inputs) {

            Log.i(
                TAG,
                "AUDIO INPUT: " +
                        "type=${device.type}, " +
                        "name=${device.productName}"
            )


            val name =
                device.productName
                    .toString()
                    .uppercase()


            val usb =
                device.type ==
                        AudioDeviceInfo.TYPE_USB_DEVICE ||
                        device.type ==
                        AudioDeviceInfo.TYPE_USB_HEADSET


            if (
                usb &&
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
         * Fallback to first USB audio input.
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

            Log.w(
                TAG,
                "AUDIO: FMA120 USB input not found"
            )


            scheduleAudioRetry(
                "FMA120 USB input missing"
            )


            return
        }


        Log.i(
            TAG,
            "AUDIO: USB INPUT FOUND: " +
                    usbInput.productName
        )


        // =====================================================
        // FIND HEADPHONE / HEARING OUTPUT
        // =====================================================

        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )


        var hearingOutput:
                AudioDeviceInfo? = null


        /*
         * Prefer hearing aid.
         */
        for (device in outputs) {

            Log.i(
                TAG,
                "AUDIO OUTPUT: " +
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
         * Normal Bluetooth headphones fallback.
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

            Log.w(
                TAG,
                "AUDIO: Bluetooth/hearing output not found"
            )


            scheduleAudioRetry(
                "Bluetooth/hearing output missing"
            )


            return
        }


        Log.i(
            TAG,
            "AUDIO: OUTPUT FOUND: " +
                    hearingOutput.productName
        )


        // =====================================================
        // START RELAY
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
                            "AUDIO ERROR: $message"
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
                "AUDIO: relay failed to start"
            )


            relay.stop()


            scheduleAudioRetry(
                "UsbAudioRelay.start() failed"
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
            "AUDIO: SUCCESS - RELAY STARTED"
        )


        Log.i(
            TAG,
            "AUDIO: ${usbInput.productName} -> " +
                    hearingOutput.productName
        )
    }


    // =========================================================
    // AUDIO RETRY
    // =========================================================

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
                "AUDIO: maximum retries reached: $reason"
            )

            return
        }


        audioRetryCount++


        Log.w(
            TAG,
            "AUDIO: $reason. Retry " +
                    "$audioRetryCount/$MAX_AUDIO_RETRIES"
        )


        mainHandler.removeCallbacks(
            audioRetryRunnable
        )


        mainHandler.postDelayed(
            audioRetryRunnable,
            AUDIO_RETRY_DELAY_MS
        )
    }


    // =========================================================
    // STOP AUDIO
    // =========================================================

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
                "AUDIO: stopping relay"
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
            "JOURNEY: ending"
        )


        journeyActive =
            false


        mainHandler.removeCallbacks(
            selectStrongestRunnable
        )


        mainHandler.removeCallbacks(
            scanWatchdogRunnable
        )


        mainHandler.removeCallbacks(
            connectionTimeoutRunnable
        )


        mainHandler.removeCallbacks(
            streamLossRunnable
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


        discoveredFmaSources.clear()


        visitedFmaBroadcastIds.clear()


        ignoredUntil.clear()


        pendingFmaSource =
            null


        activeFmaSource =
            null


        selectionScheduled =
            false


        connecting =
            false


        connected =
            false


        scanAttempt =
            0


        connectionAttempt =
            0


        state =
            null
    }


    // =========================================================
    // VIEWMODEL CLEANUP
    // =========================================================

    override fun onCleared() {

        endJourney()

        super.onCleared()
    }


    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {

        private const val TAG =
            "JourneyViewModel"


        private const val BROADCAST_CODE =
            "AURA86DEMO2026"


        /*
         * Wait this long after the first discovered source
         * so FMA120 has time to report other nearby sources.
         *
         * Then choose the strongest RSSI.
         */
        private const val SOURCE_SELECTION_WINDOW_MS =
            1_200L


        /*
         * If FMA120 reports nothing within this period,
         * restart the scan.
         */
        private const val FMA_SCAN_WINDOW_MS =
            3_000L


        /*
         * Delay between:
         *
         * BC:BI=00
         *
         * and the next:
         *
         * BC:BI
         */
        private const val FMA_SCAN_RESTART_DELAY_MS =
            300L


        /*
         * Small application-side delay between
         * stopScan() and receive().
         */
        private const val RECEIVE_AFTER_SCAN_DELAY_MS =
            250L


        /*
         * Maximum time for FMA120 to reach:
         *
         * syncState = 02
         * BIS != 0
         */
        private const val CONNECTION_TIMEOUT_MS =
            10_000L


        /*
         * Retry delay when receive fails.
         */
        private const val CONNECTION_RETRY_DELAY_MS =
            700L


        private const val MAX_CONNECTION_ATTEMPTS =
            3


        /*
         * Temporarily ignore a transmitter after
         * three failed connection attempts.
         */
        private const val FAILED_SOURCE_COOLDOWN_MS =
            10_000L


        /*
         * When current streaming disappears, wait briefly
         * before deciding that the transmitter was lost.
         */
        private const val STREAM_LOSS_CONFIRM_MS =
            1_500L


        /*
         * Wait briefly before scanning for the next source.
         */
        private const val NEXT_SOURCE_SEARCH_DELAY_MS =
            500L


        private const val AUDIO_RETRY_DELAY_MS =
            500L


        private const val MAX_AUDIO_RETRIES =
            10
    }
}