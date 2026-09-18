package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.DiscoveredBroadcast


object MatchingEngine {

    const val SEARCH_TIMEOUT = 10_000L // 10 seconds

    private const val CONNECT_TIMEOUT = 10_000L // 10 seconds



    fun decide(
        state: JourneyState,
        visible: List<DiscoveredBroadcast>,
        currentTime: Long
    ): MatchDecision {


        if (state.isJourneyOver) {
            return MatchDecision.DoNothing
        }


        if (state.phase == JourneyPhase.CONNECTING) {

            val connectingFor =
                currentTime - state.phaseStartedAt

            // FMA120 has taken too long to synchronise.
            if (connectingFor >= CONNECT_TIMEOUT) {
                return MatchDecision.Fault
            }

            // Still waiting for FMA120 synchronization.
            return MatchDecision.DoNothing
        }


        val targetStop = state.currentTargetStop

        if (targetStop == null) {
            return MatchDecision.DoNothing
        }



        if (!targetStop.hasAuracast) {

            val nextStop =
                state.nextAuracastEnabledStop

            if (
                nextStop != null &&
                visible.any {
                    StopMatcher.matches(
                        it.metadata,
                        nextStop
                    )
                }
            ) {
                return MatchDecision.Advance
            }

            return MatchDecision.DoNothing
        }


        val connectedAddress =
            state.deviceAddress


        if (connectedAddress != null) {

            // Find the next Auracast-enabled stop in the route.
            val nextStop =
                state.nextAuracastEnabledStop



            if (nextStop != null) {

                val nextVisible =
                    visible.any {
                        StopMatcher.matches(
                            it.metadata,
                            nextStop
                        )
                    }

                if (nextVisible) {
                    return MatchDecision.Advance
                }
            }



            val stillVisible =
                visible.any {
                    it.deviceAddress ==
                            connectedAddress
                }

            if (!stillVisible) {
                return MatchDecision.Disconnect
            }


            return MatchDecision.DoNothing
        }


        val match =
            visible.firstOrNull {
                StopMatcher.matches(
                    it.metadata,
                    targetStop
                )
            }


        if (match != null) {
            return MatchDecision.Connect(match)
        }


        val searchedFor =
            currentTime - state.phaseStartedAt

        if (
            state.phase == JourneyPhase.SEARCHING &&
            searchedFor >= SEARCH_TIMEOUT
        ) {
            return MatchDecision.Fault
        }



        return MatchDecision.DoNothing
    }
}