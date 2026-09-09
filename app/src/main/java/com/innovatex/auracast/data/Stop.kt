package com.innovatex.auracast.data

data class Stop(
    val id: String,
    val name: String,
    val stopLabel: String,
    val broadcast: BroadcastIdentity?
) {
    val hasAuracast: Boolean
        get() = broadcast != null
}