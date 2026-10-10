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
 * Drives a journey by telling the assistant board which stop to join and
 * reacting to what it reports back.
 */
class JourneyViewModel : ViewModel() {

    // Null until a journey is started
    var state by mutableStateOf<JourneyState?>(null)
        private set

    // If the board is reachable for the UI to show
    var boardConnected by mutableStateOf(false)
        private set

    private var boardLink: NRFBoardLink? = null

    private val tag = "JourneyViewModel"

    fun startJourney(context: Context, route: TransitRoute) {
        if (state != null) {
            return
        }

        val now = System.currentTimeMillis()
        val firstStop = route.stops.firstOrNull()

        state = JourneyState(
            route = route,
            currentStopIndex = 0,
            phase = if (firstStop?.hasAuracast == true) {
                JourneyPhase.SEARCHING
            } else {
                JourneyPhase.TRAVELLING
            },
            phaseStartedAt = now
        )

        val link = NRFBoardLink(
            context = context.applicationContext,
            onStateChanged = { onBoardStateChanged(it) },
            onConnectionChanged = { connected ->
                boardConnected = connected
                if (connected) {
                    // Board is ready, evaluate what to ask for first
                    evaluate(NRFBoardState.IDLE)
                }
            }
        )

        boardLink = link
        link.connect()
    }

    fun endJourney() {
        boardLink?.requestStop(STOP_NONE)
        boardLink?.disconnect()
        boardLink = null
        boardConnected = false
        state = null
    }

    // Called from BoardLink's GATT callback thread
    private fun onBoardStateChanged(boardState: NRFBoardState) {
        Log.i(tag, "Board reported $boardState")
        evaluate(boardState)
    }

    /**
     * Runs the engine against the board's latest report and applies the
     * decision. Called on every board state change, and once on connect.
     */
    private fun evaluate(boardState: NRFBoardState) {
        val current = state ?: return
        val now = System.currentTimeMillis()

        val decision = MatchingEngine.decide(current, boardState, now)

        Log.i(tag, "Decision: $decision")

        apply(decision, boardState, now)
    }

    private fun apply(decision: MatchDecision, boardState: NRFBoardState, now: Long) {
        val current = state ?: return

        when (decision) {
            is MatchDecision.Connect -> {
                boardLink?.requestStop(decision.stopIndex)

                state = current.copy(
                    phase = JourneyPhase.SEARCHING,
                    phaseStartedAt = now,
                    lastBoardState = NRFBoardState.IDLE,
                    requestSent = true
                )
            }

            MatchDecision.Advance -> {
                val nextIndex = current.currentStopIndex + 1
                val nextStop = current.route.stops.getOrNull(nextIndex)

                state = current.copy(
                    currentStopIndex = nextIndex,
                    phase = if (nextStop?.hasAuracast == true) {
                        JourneyPhase.SEARCHING
                    } else {
                        JourneyPhase.TRAVELLING
                    },
                    phaseStartedAt = now,
                    lastBoardState = NRFBoardState.IDLE,
                    requestSent = false
                )

                /* The engine only runs on board reports, and the board has
                 * nothing more to say until it is asked for something. Run
                 * again so the new stop's request goes out now.
                 */
                evaluate(NRFBoardState.IDLE)
            }

            MatchDecision.Disconnect -> {
                boardLink?.requestStop(STOP_NONE)

                state = current.copy(
                    phase = JourneyPhase.TRAVELLING,
                    phaseStartedAt = now,
                    lastBoardState = boardState,
                    requestSent = false
                )
            }

            MatchDecision.Fault -> {
                state = current.copy(
                    phase = JourneyPhase.DROP_OUT,
                    phaseStartedAt = now,
                    lastBoardState = boardState,
                    requestSent = false
                )
            }

            MatchDecision.DoNothing -> {
                val phase = phaseFor(boardState, current.phase)

                state = current.copy(
                    phase = phase,
                    phaseStartedAt = if (phase != current.phase) now else current.phaseStartedAt,
                    lastBoardState = boardState
                )
            }
        }
    }

    /**
     * Maps what the board reports onto what the rider is shown. Board states
     * with no journey meaning leave the current phase alone.
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
        // Board treats 0 as "stop listening"
        const val STOP_NONE = 0
    }
}