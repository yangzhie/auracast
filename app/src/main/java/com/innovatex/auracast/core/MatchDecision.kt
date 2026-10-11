package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast

// Interface for the app to create an action; NRF board carries it out
sealed interface MatchDecision {
    // Join the transmitter's broadcast
    data class Connect(val stopIndex: Int) : MatchDecision

    // Disconnect from the current broadcast
    data object Disconnect : MatchDecision

    // Nothing has changed
    data object DoNothing : MatchDecision
}