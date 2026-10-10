package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.NRFBoardState

/**
 * Decides what to ask the board for next, based on where the rider is in
 * their journey and what the board last reported.
 */
object MatchingEngine {

    // How long to wait for the board to reach a stop before giving up
    const val JOIN_TIMEOUT_MILLIS = 15_000L

    fun decide(
        state: JourneyState,
        boardState: NRFBoardState,
        nowMillis: Long
    ): MatchDecision {

        if (state.isJourneyOver) {
            return MatchDecision.DoNothing
        }

        val targetStop = state.currentTargetStop ?: return MatchDecision.DoNothing

        // The board has no sink, nothing can be joined until it does
        if (boardState == NRFBoardState.NO_SINK) {
            return MatchDecision.DoNothing
        }

        // Stop has no transmitter
        if (!targetStop.hasAuracast) {
            return MatchDecision.Advance
        }

        // Nothing requested for this stop right now
        if (!state.requestSent) {
            val broadcast = targetStop.broadcast ?: return MatchDecision.DoNothing
            return MatchDecision.Connect(broadcast.stopIndex)
        }

        // Rider was hearing this stop and no longer is; moved on
        if (state.lastBoardState == NRFBoardState.RECEIVING &&
            boardState != NRFBoardState.RECEIVING
        ) {
            return MatchDecision.Advance
        }

        if (boardState == NRFBoardState.FAILED) {
            return MatchDecision.Fault
        }

        // Asked, but the board has not reached the stop within the timeout
        val waitedFor = nowMillis - state.phaseStartedAt
        if (boardState != NRFBoardState.RECEIVING && waitedFor >= JOIN_TIMEOUT_MILLIS) {
            return MatchDecision.Fault
        }

        return MatchDecision.DoNothing
    }
}