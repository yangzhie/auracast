package com.innovatex.auracast.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataParserTest {

    @Test
    fun `decodes every field of stop 1`() {
        val result = MetadataParser.parse(hex("415501560001000101"))

        assertEquals(1, result?.protocolVersion)
        assertEquals(86, result?.routeID)
        assertEquals(1, result?.stopIndex)
        assertEquals(0, result?.direction)
        assertEquals(1, result?.language)
        assertEquals(1, result?.audioID)
    }

    @Test
    fun `decodes all four stops to the right index`() {
        assertEquals(1, MetadataParser.parse(hex("415501560001000101"))?.stopIndex)
        assertEquals(2, MetadataParser.parse(hex("415501560002000102"))?.stopIndex)
        assertEquals(3, MetadataParser.parse(hex("415501560003000103"))?.stopIndex)
        assertEquals(4, MetadataParser.parse(hex("415501560004000104"))?.stopIndex)
    }

    @Test
    fun `route id is little-endian`() {
        // 0x56 0x00 reads as 86. Byte-swapped it would be 22016.
        assertEquals(86, MetadataParser.parse(hex("415501560001000101"))?.routeID)
    }

    @Test
    fun `rejects payload shorter than nine bytes`() {
        assertNull(MetadataParser.parse(hex("41550156")))
    }

    @Test
    fun `rejects empty payload`() {
        assertNull(MetadataParser.parse(ByteArray(0)))
    }

    @Test
    fun `rejects wrong magic bytes`() {
        // "XY" instead of "AU"
        assertNull(MetadataParser.parse(hex("585901560001000101")))
    }

    @Test
    fun `rejects unknown protocol version`() {
        // version 2
        assertNull(MetadataParser.parse(hex("415502560001000101")))
    }

    /** Turns "4155" into a ByteArray of 0x41, 0x55. */
    private fun hex(s: String): ByteArray =
        s.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
}