package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast
import com.innovatex.auracast.data.Stop

/**
 * Decides which stop's broadcast the board should be joined to, based on
 * which transmitters the phone can currently hear.
 *
 * A transmitter only reaches its own platform, so hearing one is taken as
 * the rider being at that stop. The expected next stop is preferred, but
 * any project transmitter with a strong signal is accepted — a rider may
 * board mid-route, skip a stop, or pass one that was never fitted.
 *
 * Pure logic: no Android APIs, no side effects.
 */
object MatchingEngine {

    /**
     * A transmitter weaker than this is treated as overheard from
     * somewhere else rather than as the rider's current stop.
     */
    const val MIN_ACCEPTABLE_RSSI = -85

    fun decide(
        state: JourneyState,
        visible: List<DiscoveredBroadcast>,
        nowMillis: Long
    ): MatchDecision {
        if (state.isJourneyOver) {
            return MatchDecision.DoNothing
        }

        val onRoute = visible.filter { belongsToRoute(it, state.route.stops) }

        /* Still hearing what we are joined to — the rider has not moved on. */
        val joined = state.joinedStopIndex
        if (joined != null && onRoute.any { it.metadata.stopIndex == joined }) {
            return MatchDecision.DoNothing
        }

        /* The stop the journey expects next, if we can hear it. */
        val expected = state.currentTargetStop
        if (expected != null) {
            val match = onRoute.firstOrNull { StopMatcher.matches(it.metadata, expected) }

            if (match != null) {
                return MatchDecision.Connect(match.metadata.stopIndex)
            }
        }

        /* Not the expected stop, but something on this route is close enough
         * to be where the rider actually is.
         */
        val strongest = onRoute
            .filter { it.rssi >= MIN_ACCEPTABLE_RSSI }
            .maxByOrNull { it.rssi }

        if (strongest != null) {
            return MatchDecision.Connect(strongest.metadata.stopIndex)
        }

        /* Nothing audible. If something was playing, the rider has left. */
        if (joined != null) {
            return MatchDecision.Disconnect
        }

        return MatchDecision.DoNothing
    }

    /**
     * Whether a transmitter belongs to any stop on this route. Guards
     * against joining another route's announcements at an interchange.
     */
    private fun belongsToRoute(
        broadcast: DiscoveredBroadcast,
        stops: List<Stop>
    ): Boolean = stops.any { StopMatcher.matches(broadcast.metadata, it) }
}