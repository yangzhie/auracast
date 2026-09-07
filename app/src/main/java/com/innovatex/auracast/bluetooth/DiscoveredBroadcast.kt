package com.innovatex.auracast.bluetooth

// A transmitter currently being seen by the scanner
data class DiscoveredBroadcast(
    val deviceAddress: String,
    val broadcastName: String?,
    val rssi: Int,
    val metadata: BroadcastMetadata,
    val lastSeenMillis: Long
)