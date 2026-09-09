package com.innovatex.auracast.core

import com.innovatex.auracast.data.Stop
import com.innovatex.auracast.data.TransitRoute

enum class JourneyPhase {
    SEARCHING,
    RECEIVING,
    TRAVELLING,
    AT_UNCOVERED,
    DROP_OUT
}

data class JourneyState(
    val route: TransitRoute,
    val currentStopIndex: Int,
    val phase: JourneyPhase,
    val deviceAddress: String?,
    val phaseStartedAt: Long
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