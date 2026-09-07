package com.innovatex.auracast.bluetooth

data class BroadcastMetadata(
    val protocolVersion: Int,
    val routeID: Int,
    val stopIndex: Int,
    val direction: Int,
    val language: Int,
    val audioID: Int
) {}