package com.innovatex.auracast.data

data class BroadcastIdentity(
    val routeID: Int,
    val stopIndex: Int,
    val direction: Int = DIRECTION_OUTBOUND,
    val language: Int = DIRECTION_INBOUND,
) {
    companion object {
        const val DIRECTION_OUTBOUND = 0
        const val DIRECTION_INBOUND = 1
        const val LANGUAGE_ENGLISH = 1
    }
}