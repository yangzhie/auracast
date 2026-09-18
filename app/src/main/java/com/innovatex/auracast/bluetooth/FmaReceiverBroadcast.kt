package com.innovatex.auracast.bluetooth

data class FmaReceiverBroadcast(
    val addressType: Int,
    val address: String,
    val rssi: Int,

    // Keep the original 8 hex characters too.
    val broadcastIDs: String,

    val advertisingSid: Int,
    val broadcastId: Int,

    val broadcastName: String
)