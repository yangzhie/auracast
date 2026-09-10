package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast

object MatchingEngine {
    const val SEARCH_TIMEOUT = 10_000L // 10s timeout

    fun decide(
        state: JourneyState,
        visible: List<DiscoveredBroadcast>,
        currentTime: Long
    ): MatchDecision {

        // Case: route/journey is over -> do nothing
        if (state.isJourneyOver) {
            return MatchDecision.DoNothing
        }

        // Case: target stop is non-existant -> do nothing
        val targetStop = state.currentTargetStop
        if (targetStop == null) {
            return MatchDecision.DoNothing
        }

        // Case: the stop has no Auracast transmitter -> do nothing
        if (!targetStop.hasAuracast) {
            // First check if next covered stop has shown up
            val nextStop = state.nextAuracastEnabledStop
            if (nextStop != null && visible.any { StopMatcher.matches(it.metadata, nextStop) }) {
                return MatchDecision.Advance
            }

            return MatchDecision.DoNothing
        }

        // Get device addr
        val connectedAddress = state.deviceAddress
        // Case: device is connected
        if (connectedAddress != null) {
            // Get next valid stop
            val nextStop = state.nextAuracastEnabledStop
            // Case: next stop has appeared -> advance
            if (nextStop != null) {
                val nextVisible = visible.any { StopMatcher.matches(it.metadata, nextStop) }
                if (nextVisible) {
                    return MatchDecision.Advance
                }
            }

            // Case: transmitter we're joined to is no longer in range -> dc
            val stillVisible = visible.any { it.deviceAddress == connectedAddress }
            if (!stillVisible) {
                return MatchDecision.Disconnect
            }

            return MatchDecision.DoNothing
        }

        // Case: not connected & if expected transmitter is in range -> connect
        val match = visible.firstOrNull { StopMatcher.matches(it.metadata, targetStop) }
        if (match != null) {
            return MatchDecision.Connect(match)
        }

        // Case: timed out -> fault
        val searchedFor = currentTime - state.phaseStartedAt
        if (state.phase == JourneyPhase.SEARCHING && searchedFor >= SEARCH_TIMEOUT) {
            return MatchDecision.Fault
        }

        // Case: still looking
        return MatchDecision.DoNothing
    }
}