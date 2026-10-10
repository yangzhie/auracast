package com.innovatex.auracast.core

import com.innovatex.auracast.data.Stop
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.bluetooth.NRFBoardState

enum class JourneyPhase {
    SEARCHING, // Board asks to find stop's transmitter
    CONNECTING, // Board joins transmitter, waiting to sync
    RECEIVING, // Audio playing
    TRAVELLING, // Between stops, nothing to hear
    AT_UNCOVERED, // Stop with no transmitter fitted
    DROP_OUT // Lost the stop / board failure
}

data class JourneyState(
    val route: TransitRoute,
    val currentStopIndex: Int,
    val phase: JourneyPhase,
    val phaseStartedAt: Long,
    val lastBoardState: NRFBoardState = NRFBoardState.IDLE, // Change can be detected
    val requestSent: Boolean = false
) {
    // Stop at currentStopIndex
    val currentTargetStop: Stop?
        get() = route.stops.getOrNull(currentStopIndex)

    // Next stop with coverage
    val nextAuracastEnabledStop: Stop?
        get() = route.stops.drop(currentStopIndex + 1).firstOrNull { it.hasAuracast }

    // Is the journey over?
    val isJourneyOver: Boolean
        get() = currentTargetStop == null
}