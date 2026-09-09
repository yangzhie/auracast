package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast

// Interface for the app to create an action
sealed interface MatchDecision {
    // Join the transmitter's broadcast
    data class Connect(val broadcast: DiscoveredBroadcast) : MatchDecision

    // Disconnect from the current broadcast
    data object Disconnect : MatchDecision

    // Move onto the next broadcast
    data object Advance : MatchDecision

    // Nothing has changed
    data object DoNothing : MatchDecision

    // Returns an error
    data object Fault : MatchDecision
}