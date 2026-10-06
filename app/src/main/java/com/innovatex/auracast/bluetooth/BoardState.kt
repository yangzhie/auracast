package com.innovatex.auracast.bluetooth

/**
 * Board's reported state, mirroring enum gatt_link_state.
 */
enum class BoardState {
    IDLE,
    SCANNING,
    CONNECTING,
    RECEIVING,
    NO_SINK,
    FAILED;

    companion object {
        // Converts a notified byte, or null if the board sent something unknown
        fun fromByte(value: Int): BoardState? = entries.getOrNull(value)
    }
}