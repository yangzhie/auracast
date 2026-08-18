package com.innovatex.auracast.data

data class Stop(
    val id: String,
    val name: String,
    val stopLabel: String,
    val broadcastId: String?
) {
    val hasAuracast: Boolean
        get() = broadcastId != null
}