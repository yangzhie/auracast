package com.innovatex.auracast.bluetooth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.core.MatchDecision
import com.innovatex.auracast.core.MatchingEngine
import com.innovatex.auracast.data.TransitRoute

/**
 * Drives a journey. The phone scans for transmitters and decides which
 * stop the rider is at; the board is told which one to join and reports
 * back only so the UI can show whether audio is actually flowing.
 */
class JourneyViewModel : ViewModel() {

    // Null until a journey is started
    var state by mutableStateOf<JourneyState?>(null)
        private set

    // Whether the board is reachable, for the UI to show
    var boardConnected by mutableStateOf(false)
        private set

    private var boardLink: NRFBoardLink? = null
    private var scanner: BroadcastScanner? = null

    private var started = false

    /* Transmitters currently audible, keyed by address. Entries age out,
     * which is how the phone notices the rider has left a stop.
     */
    private var discovered: Map<String, DiscoveredBroadcast> = emptyMap()

    private val tag = "JourneyViewModel"

    fun startJourney(context: Context, route: TransitRoute) {
        if (started) {
            Log.w(tag, "startJourney called again, ignoring")
            return
        }
        started = true

        val now = System.currentTimeMillis()

        state = JourneyState(
            route = route,
            currentStopIndex = 0,
            phase = JourneyPhase.SEARCHING,
            phaseStartedAt = now
        )

        val link = NRFBoardLink(
            context = context.applicationContext,
            onStateChanged = { onBoardStateChanged(it) },
            onConnectionChanged = { connected ->
                boardConnected = connected
                if (connected) {
                    /* The board is ready; act on whatever we can already
                     * hear rather than waiting for the next advertisement.
                     */
                    evaluate()
                }
            }
        )

        boardLink = link
        link.connect()

        val newScanner = BroadcastScanner(
            context = context.applicationContext,
            onBroadcastFound = { result, metadata ->
                onBroadcastFound(
                    address = result.device.address,
                    name = result.scanRecord?.deviceName,
                    rssi = result.rssi,
                    metadata = metadata
                )
            }
        )

        newScanner.start()
        scanner = newScanner
    }

    fun endJourney() {
        started = false

        scanner?.stop()
        scanner = null

        boardLink?.requestStop(STOP_NONE)
        boardLink?.disconnect()
        boardLink = null

        boardConnected = false
        discovered = emptyMap()
        state = null
    }

    /** Called on the scanner's thread for every matching advertisement. */
    private fun onBroadcastFound(
        address: String,
        name: String?,
        rssi: Int,
        metadata: BroadcastMetadata
    ) {
        val now = System.currentTimeMillis()

        val updated = discovered.toMutableMap()

        updated[address] = DiscoveredBroadcast(
            deviceAddress = address,
            broadcastName = name,
            rssi = rssi,
            metadata = metadata,
            lastSeenMillis = now
        )

        /* Anything not heard recently is out of range. This expiry is what
         * tells the engine the rider has moved away from a stop.
         */
        updated.entries.removeAll { now - it.value.lastSeenMillis > STALE_AFTER_MILLIS }

        discovered = updated

        evaluate()
    }

    /** Called from the board's GATT callback thread. */
    private fun onBoardStateChanged(boardState: NRFBoardState) {
        Log.i(tag, "Board reported $boardState")

        val current = state ?: return
        val phase = phaseFor(boardState, current.phase)
        val now = System.currentTimeMillis()

        state = current.copy(
            phase = phase,
            phaseStartedAt = if (phase != current.phase) now else current.phaseStartedAt
        )
    }

    /** Runs the engine against what is currently audible. */
    private fun evaluate() {
        val current = state ?: return

        /* Deciding before the board can receive the write wedges the
         * engine: the decision is recorded as joined, the write is
         * dropped, and nothing reconsiders it.
         */
        if (!boardConnected) {
            return
        }

        val now = System.currentTimeMillis()

        val audible = discovered.values.toList()
        val decision = MatchingEngine.decide(current, audible, now)

        if (decision !is MatchDecision.DoNothing) {
            Log.i(tag, "Decision: $decision (${audible.size} audible)")
        }

        apply(decision, now)
    }

    private fun apply(decision: MatchDecision, now: Long) {
        val current = state ?: return

        when (decision) {
            is MatchDecision.Connect -> {
                boardLink?.requestStop(decision.stopIndex)

                /* Move the journey to whichever stop carries this
                 * transmitter, so the UI names the right platform.
                 */
                val routeIndex = current.route.stops.indexOfFirst {
                    it.broadcast?.stopIndex == decision.stopIndex
                }

                state = current.copy(
                    currentStopIndex = if (routeIndex >= 0) routeIndex else current.currentStopIndex,
                    phase = JourneyPhase.SEARCHING,
                    phaseStartedAt = now,
                    joinedStopIndex = decision.stopIndex
                )
            }

            MatchDecision.Disconnect -> {
                boardLink?.requestStop(STOP_NONE)

                state = current.copy(
                    phase = JourneyPhase.TRAVELLING,
                    phaseStartedAt = now,
                    joinedStopIndex = null
                )
            }

            MatchDecision.DoNothing -> {
                /* Nothing to do — the board's own reports drive the phase. */
            }
        }
    }

    /**
     * Maps what the board reports onto what the rider is shown. States with
     * no journey meaning leave the current phase alone.
     */
    private fun phaseFor(boardState: NRFBoardState, currentPhase: JourneyPhase): JourneyPhase =
        when (boardState) {
            NRFBoardState.SCANNING -> JourneyPhase.SEARCHING
            NRFBoardState.CONNECTING -> JourneyPhase.CONNECTING
            NRFBoardState.RECEIVING -> JourneyPhase.RECEIVING
            NRFBoardState.FAILED -> JourneyPhase.DROP_OUT
            NRFBoardState.NO_SINK -> JourneyPhase.DROP_OUT
            NRFBoardState.IDLE -> currentPhase
        }

    override fun onCleared() {
        endJourney()
    }

    private companion object {
        /** The board treats 0 as "stop listening". */
        const val STOP_NONE = 0

        /** How long a transmitter can go unheard before it counts as gone. */
        const val STALE_AFTER_MILLIS = 15_000L
    }
}