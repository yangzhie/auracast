package com.innovatex.auracast.bluetooth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.innovatex.auracast.audio.UsbAudioRelay
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.core.MatchDecision
import com.innovatex.auracast.core.MatchingEngine
import com.innovatex.auracast.data.TransitRoute

class JourneyViewModel : ViewModel() {

    var state by mutableStateOf<JourneyState?>(null)
        private set

    private var scanner: BroadcastScanner? = null

    private var fmaReceiver: Fma120ReceiverController? = null

    private var audioRelay: UsbAudioRelay? = null

    private val visible =
        mutableMapOf<String, DiscoveredBroadcast>()

    private val fmaVisible =
        mutableMapOf<String, FmaReceiverBroadcast>()

    private var pendingBroadcast:
            DiscoveredBroadcast? = null

    private val staleAfterMillis =
        5_000L

    fun startJourney(
        context: Context,
        route: TransitRoute
    ) {

        if (state != null) {
            return
        }

        val appContext =
            context.applicationContext

        val firstStop =
            route.stops.firstOrNull()

        var initialPhase =
            JourneyPhase.AT_UNCOVERED

        if (firstStop != null) {

            if (firstStop.hasAuracast) {

                initialPhase =
                    JourneyPhase.SEARCHING
            }
        }

        state =
            JourneyState(
                route = route,
                currentStopIndex = 0,
                phase = initialPhase,
                deviceAddress = null,
                phaseStartedAt =
                    System.currentTimeMillis()
            )

        val receiver =
            Fma120ReceiverController(
                context = appContext,

                onBroadcastFound = {
                        source ->

                    onFmaBroadcastFound(
                        source
                    )
                },

                onReceiveStateChanged = {
                        receiveState ->

                    onFmaReceiveStateChanged(
                        receiveState
                    )
                },

                onError = {
                        message ->

                    Log.e(
                        TAG,
                        "FMA120 error: $message"
                    )
                }
            )

        fmaReceiver =
            receiver

        val receiverOpened =
            receiver.open()

        if (receiverOpened) {

            receiver.startScan()

        } else {

            Log.e(
                TAG,
                "Unable to open FMA120 receiver."
            )
        }

        val newScanner =
            BroadcastScanner(
                context = appContext,

                onBroadcastFound = {
                        result,
                        metadata ->

                    onScanResult(
                        address =
                            result.device.address,

                        name =
                            result.scanRecord
                                ?.deviceName,

                        rssi =
                            result.rssi,

                        metadata =
                            metadata
                    )
                }
            )

        newScanner.start()

        scanner =
            newScanner

        Log.i(
            TAG,
            "Journey started."
        )
    }

    fun endJourney() {

        Log.i(
            TAG,
            "Ending journey"
        )

        leaveBroadcast()

        val currentScanner =
            scanner

        if (currentScanner != null) {

            currentScanner.stop()

            scanner =
                null
        }

        val currentReceiver =
            fmaReceiver

        if (currentReceiver != null) {

            currentReceiver.stopScan()

            currentReceiver.close()

            fmaReceiver =
                null
        }

        val currentRelay =
            audioRelay

        if (currentRelay != null) {

            currentRelay.stop()

            audioRelay =
                null
        }

        visible.clear()

        fmaVisible.clear()

        pendingBroadcast =
            null

        state =
            null
    }

    private fun onScanResult(
        address: String,
        name: String?,
        rssi: Int,
        metadata: BroadcastMetadata
    ) {

        val now =
            System.currentTimeMillis()

        visible[address] =
            DiscoveredBroadcast(
                deviceAddress =
                    address,

                broadcastName =
                    name,

                rssi =
                    rssi,

                metadata =
                    metadata,

                lastSeenMillis =
                    now
            )

        val iterator =
            visible.entries.iterator()

        while (iterator.hasNext()) {

            val entry =
                iterator.next()

            val age =
                now -
                        entry.value.lastSeenMillis

            if (age > staleAfterMillis) {

                iterator.remove()
            }
        }

        val current =
            state

        if (current == null) {
            return
        }

        val decision =
            MatchingEngine.decide(
                state =
                    current,

                visible =
                    visible.values.toList(),

                currentTime =
                    now
            )

        apply(
            decision =
                decision,

            now =
                now
        )
    }

    private fun apply(
        decision: MatchDecision,
        now: Long
    ) {

        val current =
            state

        if (current == null) {
            return
        }

        when (decision) {

            is MatchDecision.Connect -> {

                Log.i(
                    TAG,
                    "MatchingEngine selected stop " +
                            "${decision.broadcast.metadata.stopIndex}"
                )

                state =
                    current.copy(
                        deviceAddress =
                            decision.broadcast
                                .deviceAddress,

                        phase =
                            JourneyPhase.CONNECTING,

                        phaseStartedAt =
                            now
                    )

                joinBroadcast(
                    decision.broadcast
                )
            }

            MatchDecision.Disconnect -> {

                leaveBroadcast()

                state =
                    current.copy(
                        deviceAddress =
                            null,

                        phase =
                            JourneyPhase.SEARCHING,

                        phaseStartedAt =
                            now
                    )
            }

            MatchDecision.Advance -> {

                leaveBroadcast()

                val nextIndex =
                    current.currentStopIndex + 1

                val nextStop =
                    current.route.stops
                        .getOrNull(
                            nextIndex
                        )

                var nextPhase =
                    JourneyPhase.AT_UNCOVERED

                if (nextStop != null) {

                    if (nextStop.hasAuracast) {

                        nextPhase =
                            JourneyPhase.SEARCHING
                    }
                }

                state =
                    current.copy(
                        currentStopIndex =
                            nextIndex,

                        deviceAddress =
                            null,

                        phase =
                            nextPhase,

                        phaseStartedAt =
                            now
                    )
            }

            MatchDecision.Fault -> {

                Log.e(
                    TAG,
                    "Journey entered DROP_OUT state"
                )

                state =
                    current.copy(
                        deviceAddress =
                            null,

                        phase =
                            JourneyPhase.DROP_OUT,

                        phaseStartedAt =
                            now
                    )
            }

            MatchDecision.DoNothing -> {

                return
            }
        }
    }

    private fun joinBroadcast(
        broadcast: DiscoveredBroadcast
    ) {

        Log.i(
            TAG,
            "FMA join requested: " +
                    "name=${broadcast.broadcastName}, " +
                    "address=${broadcast.deviceAddress}, " +
                    "stop=${broadcast.metadata.stopIndex}, " +
                    "rssi=${broadcast.rssi}"
        )

        val source =
            FmaSourceResolver.resolve(
                androidBroadcast =
                    broadcast,

                fmaBroadcasts =
                    fmaVisible.values
            )

        if (source == null) {

            Log.i(
                TAG,
                "Correct stop identified by Android, " +
                        "but matching FMA source has not been discovered yet."
            )

            pendingBroadcast =
                broadcast

            val currentReceiver =
                fmaReceiver

            if (currentReceiver != null) {

                currentReceiver.startScan()
            }

            return
        }

        pendingBroadcast =
            null

        Log.i(
            TAG,
            "FMA source resolved: " +
                    "SID=${source.advertisingSid}, " +
                    "BroadcastID=${source.broadcastId}, " +
                    "name=${source.broadcastName}"
        )

        val currentReceiver =
            fmaReceiver

        if (currentReceiver == null) {

            Log.e(
                TAG,
                "Cannot receive broadcast: " +
                        "FMA120 receiver is not available."
            )

            setDropOut()

            return
        }

        currentReceiver.receive(
            source
        )
    }

    private fun onFmaBroadcastFound(
        source: FmaReceiverBroadcast
    ) {

        val normalizedAddress =
            normalizeAddress(
                source.address
            )

        fmaVisible[normalizedAddress] =
            source

        Log.i(
            TAG,
            "FMA broadcast found: " +
                    "name=${source.broadcastName}, " +
                    "address=$normalizedAddress, " +
                    "SID=${source.advertisingSid}, " +
                    "BroadcastID=${source.broadcastId}"
        )

        val pending =
            pendingBroadcast

        if (pending == null) {
            return
        }

        val resolved =
            FmaSourceResolver.resolve(
                androidBroadcast =
                    pending,

                fmaBroadcasts =
                    fmaVisible.values
            )

        if (resolved != null) {

            Log.i(
                TAG,
                "Pending Android broadcast matched " +
                        "with FMA receiver source."
            )

            pendingBroadcast =
                null

            val currentReceiver =
                fmaReceiver

            if (currentReceiver == null) {

                Log.e(
                    TAG,
                    "FMA120 receiver disappeared " +
                            "before source could be selected."
                )

                setDropOut()

                return
            }

            currentReceiver.receive(
                resolved
            )
        }
    }

    private fun onFmaReceiveStateChanged(
        report: FmaReceiveState
    ) {

        Log.i(
            TAG,
            "FMA receive state: " +
                    "source=${report.sourceId}, " +
                    "sync=${report.syncState}, " +
                    "encryption=${report.encryptionState}, " +
                    "BIS=${report.bisState}"
        )

        if (report.needsBroadcastCode) {

            Log.i(
                TAG,
                "FMA120 requested Broadcast Code."
            )

            val currentReceiver =
                fmaReceiver

            if (currentReceiver == null) {

                Log.e(
                    TAG,
                    "Cannot provide Broadcast Code: " +
                            "receiver is unavailable."
                )

                setDropOut()

                return
            }

            currentReceiver.provideBroadcastCode(
                sourceId =
                    report.sourceId,

                code =
                    BROADCAST_CODE
            )

            return
        }

        if (report.syncFailed) {

            Log.e(
                TAG,
                "FMA120 failed to synchronize."
            )

            val current =
                state

            if (current == null) {
                return
            }

            state =
                current.copy(
                    deviceAddress =
                        null,

                    phase =
                        JourneyPhase.DROP_OUT,

                    phaseStartedAt =
                        System.currentTimeMillis()
                )

            return
        }

        if (report.isStreaming) {

            Log.i(
                TAG,
                "FMA120 is synchronized and " +
                        "Auracast audio is streaming."
            )

            val current =
                state

            if (current == null) {
                return
            }

            state =
                current.copy(
                    phase =
                        JourneyPhase.RECEIVING,

                    phaseStartedAt =
                        System.currentTimeMillis()
                )

            return
        }
    }

    private fun leaveBroadcast() {

        Log.i(
            TAG,
            "Stopping FMA broadcast reception"
        )

        val currentReceiver =
            fmaReceiver

        if (currentReceiver != null) {

            currentReceiver.stopReceiving()
        }

        val currentRelay =
            audioRelay

        if (currentRelay != null) {

            currentRelay.stop()

            audioRelay =
                null
        }

        pendingBroadcast =
            null
    }

    private fun setDropOut() {

        val current =
            state

        if (current == null) {
            return
        }

        state =
            current.copy(
                deviceAddress =
                    null,

                phase =
                    JourneyPhase.DROP_OUT,

                phaseStartedAt =
                    System.currentTimeMillis()
            )
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

    override fun onCleared() {

        endJourney()

        super.onCleared()
    }

    companion object {

        private const val TAG =
            "JourneyViewModel"

        private const val BROADCAST_CODE =
            "AURA86DEMO2026"
    }
}