package com.innovatex.auracast.bluetooth

data class FmaReceiveState(
    val sourceId: Int,
    val broadcastIDs: String,
    val syncState: Int,
    val encryptionState: Int,
    val bisState: Int
) {

    val isStreaming: Boolean
        get() =
            syncState == 0x02 &&
                    bisState != 0

    val needsBroadcastCode: Boolean
        get() =
            encryptionState == 0x01

    val syncFailed: Boolean
        get() =
            syncState == 0x03
}