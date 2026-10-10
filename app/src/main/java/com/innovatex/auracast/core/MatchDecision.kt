package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast

// Interface for the app to create an action; NRF board carries it out
sealed interface MatchDecision {
    // Join the transmitter's broadcast
    data class Connect(val stopIndex: Int) : MatchDecision

    // Disconnect from the current broadcast
    data object Disconnect : MatchDecision

    // Move onto the next broadcast
    data object Advance : MatchDecision

    // Nothing has changed
    data object DoNothing : MatchDecision

    // Returns an error
    data object Fault : MatchDecision
}