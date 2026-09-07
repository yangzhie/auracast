package com.innovatex.auracast.bluetooth

object MetadataParser {
    public const val COMPANY_ID = 0xFFFF // Reserved ID for testing purposes
    private const val MAGIC_A = 0x41 // A
    private const val MAGIC_U = 0x55 // U
    private const val MIN_LEN = 9 // Payload's length
    private const val SUPPORTED_VER = 1 // Current version

    // Parsing
    fun parse(data: ByteArray): BroadcastMetadata? {
        // Check: if data is less than expected metadata length
        if (data.size < MIN_LEN) {
            return null
        }

        // Check: the payload identifiers
        if (byteAt(data, 0) != MAGIC_A || byteAt(data, 1) != MAGIC_U) {
            return null
        }

        // Check: payload's version against current
        if (byteAt(data, 2) != SUPPORTED_VER) {
            return null
        }

        val payload = BroadcastMetadata(
            protocolVersion = byteAt(data, 2),
            routeID = littleEndianShortAt(data, 3),
            stopIndex = byteAt(data, 5),
            direction = byteAt(data, 6),
            language = byteAt(data, 7),
            audioID = byteAt(data, 8)
        )

        return payload
    }
}

// Obtain payload data at a byte-level
private fun byteAt(data: ByteArray, index: Int): Int {
    // Get signed data
    val signed = data[index]
    // Convert to int
    val widened = signed.toInt()
    // Convert to unsigned
    val unsigned = widened and 0xFF

    return unsigned
}


// Obtain little endian's payload data at an index
private fun littleEndianShortAt(data: ByteArray, index: Int): Int {
    // Obtain lowest byte
    val lowByte = byteAt(data, index)
    // Obtain highest byte
    val highByte = byteAt(data, index + 1)
    // Shift every 8th bit left (multiply by 256)
    val shiftedHigh = highByte shl 8

    return lowByte or shiftedHigh
}