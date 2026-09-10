package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.BroadcastMetadata
import com.innovatex.auracast.bluetooth.DiscoveredBroadcast
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.data.TransitRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingEngineTest {

    // ---------- searching ----------

    @Test
    fun `connects when the expected transmitter is visible`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 0),
            visible = listOf(broadcast(stopIndex = 1)),
            currentTime = 1_000L
        )

        assertTrue(decision is MatchDecision.Connect)
    }

    @Test
    fun `ignores a transmitter belonging to a different stop`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 0),
            visible = listOf(broadcast(stopIndex = 3)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    @Test
    fun `keeps searching before the timeout`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 0, phaseStartedAt = 0L),
            visible = emptyList(),
            currentTime = 5_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    @Test
    fun `reports a fault once the timeout has passed`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 0, phaseStartedAt = 0L),
            visible = emptyList(),
            currentTime = 11_000L
        )

        assertEquals(MatchDecision.Fault, decision)
    }

    // ---------- connected ----------

    @Test
    fun `stays put while the current transmitter is still in range`() {
        val decision = MatchingEngine.decide(
            state = connectedAt(stopIndex = 0),
            visible = listOf(broadcast(stopIndex = 1, address = CONNECTED_ADDRESS)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    @Test
    fun `disconnects when the current transmitter drops out of range`() {
        val decision = MatchingEngine.decide(
            state = connectedAt(stopIndex = 0),
            visible = emptyList(),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.Disconnect, decision)
    }

    @Test
    fun `advances when the next covered stop's transmitter appears`() {
        val decision = MatchingEngine.decide(
            state = connectedAt(stopIndex = 0),
            visible = listOf(broadcast(stopIndex = 2)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.Advance, decision)
    }

    @Test
    fun `prefers advancing over disconnecting when both transmitters are visible`() {
        // The tram is pulling into the next stop while the previous
        // transmitter is still in range. Advance carries more information
        // than a bare disconnect, so it must win.
        val decision = MatchingEngine.decide(
            state = connectedAt(stopIndex = 0),
            visible = listOf(
                broadcast(stopIndex = 1, address = CONNECTED_ADDRESS),
                broadcast(stopIndex = 2, address = "BB:BB:BB:BB:BB:BB")
            ),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.Advance, decision)
    }

    // ---------- nothing to do ----------

    @Test
    fun `does nothing at a stop with no transmitter`() {
        // Index 1 is Nicholson Street, which is not fitted.
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 1),
            visible = listOf(broadcast(stopIndex = 1)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    @Test
    fun `does nothing once the journey has ended`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = route.stops.size),
            visible = listOf(broadcast(stopIndex = 1)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    @Test
    fun `connects at a later stop, not just the first`() {
        // Index 4 is Johnston Street, transmitter 3.
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 4),
            visible = listOf(broadcast(stopIndex = 3)),
            currentTime = 1_000L
        )

        assertTrue(decision is MatchDecision.Connect)
    }

    @Test
    fun `advances from an uncovered stop when the next covered transmitter appears`() {
        // Index 3 is Langridge Street, unfitted. Index 4 is Johnston, transmitter 3.
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 3),
            visible = listOf(broadcast(stopIndex = 3)),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.Advance, decision)
    }

    @Test
    fun `waits at an uncovered stop while nothing is visible`() {
        val decision = MatchingEngine.decide(
            state = searchingAt(stopIndex = 3),
            visible = emptyList(),
            currentTime = 1_000L
        )

        assertEquals(MatchDecision.DoNothing, decision)
    }

    // ---------- helpers ----------

    private companion object {
        const val CONNECTED_ADDRESS = "AA:AA:AA:AA:AA:AA"
    }

    private val route: TransitRoute = SampleData.routes.first()

    /** Not connected to anything, actively looking. */
    private fun searchingAt(stopIndex: Int, phaseStartedAt: Long = 0L) = JourneyState(
        route = route,
        currentStopIndex = stopIndex,
        phase = JourneyPhase.SEARCHING,
        deviceAddress = null,
        phaseStartedAt = phaseStartedAt
    )

    /** Joined to CONNECTED_ADDRESS at the given stop. */
    private fun connectedAt(stopIndex: Int, phaseStartedAt: Long = 0L) = JourneyState(
        route = route,
        currentStopIndex = stopIndex,
        phase = JourneyPhase.RECEIVING,
        deviceAddress = CONNECTED_ADDRESS,
        phaseStartedAt = phaseStartedAt
    )

    /** A transmitter in range, advertising the given stop index. */
    private fun broadcast(
        stopIndex: Int,
        address: String = CONNECTED_ADDRESS,
        rssi: Int = -60
    ) = DiscoveredBroadcast(
        deviceAddress = address,
        broadcastName = "AURA86-S$stopIndex",
        rssi = rssi,
        metadata = BroadcastMetadata(
            protocolVersion = 1,
            routeID = 86,
            stopIndex = stopIndex,
            direction = 0,
            language = 1,
            audioID = stopIndex
        ),
        lastSeenMillis = 0L
    )
}