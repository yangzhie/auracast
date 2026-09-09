package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.BroadcastMetadata
import com.innovatex.auracast.data.BroadcastIdentity
import com.innovatex.auracast.data.Stop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopMatcherTest {

    @Test
    fun `matches when every field agrees`() {
        assertTrue(StopMatcher.matches(metadata(), stop()))
    }

    @Test
    fun `rejects a different route`() {
        assertFalse(StopMatcher.matches(metadata(routeID = 96), stop()))
    }

    @Test
    fun `rejects a different stop index`() {
        assertFalse(StopMatcher.matches(metadata(stopIndex = 3), stop()))
    }

    @Test
    fun `rejects the opposite direction`() {
        // Same physical stop, opposite platform — different announcements.
        assertFalse(StopMatcher.matches(metadata(direction = 1), stop()))
    }

    @Test
    fun `rejects a different language`() {
        assertFalse(StopMatcher.matches(metadata(language = 2), stop()))
    }

    @Test
    fun `rejects a stop with no transmitter`() {
        assertFalse(StopMatcher.matches(metadata(), stop(broadcast = null)))
    }

    @Test
    fun `matches a stop other than the first`() {
        val johnston = stop(broadcast = BroadcastIdentity(routeID = 86, stopIndex = 3))

        assertTrue(StopMatcher.matches(metadata(stopIndex = 3), johnston))
        assertFalse(StopMatcher.matches(metadata(stopIndex = 2), johnston))
    }

    /** Metadata as a provisioned transmitter would advertise it. */
    private fun metadata(
        routeID: Int = 86,
        stopIndex: Int = 1,
        direction: Int = 0,
        language: Int = 1
    ) = BroadcastMetadata(
        protocolVersion = 1,
        routeID = routeID,
        stopIndex = stopIndex,
        direction = direction,
        language = language,
        audioID = stopIndex
    )

    /** A stop, fitted by default. Pass null for an uncovered one. */
    private fun stop(
        broadcast: BroadcastIdentity? = BroadcastIdentity(routeID = 86, stopIndex = 1)
    ) = Stop(
        id = "s8",
        name = "Parliament",
        stopLabel = "Stop 8 · Spring St",
        broadcast = broadcast
    )
}