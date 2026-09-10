package com.innovatex.auracast.bluetooth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.core.MatchDecision
import com.innovatex.auracast.core.MatchingEngine
import com.innovatex.auracast.data.TransitRoute

// Journey driver - applies whatever MatchingEngine decides
class JourneyViewModel : ViewModel() {
    // Null until journey starts
    var state by mutableStateOf<JourneyState?>(null)
        private set

    // Persistent scanner
    private var scanner: BroadcastScanner? = null

    // Detected transmitters
    private val visible = mutableMapOf<String, DiscoveredBroadcast>()

    // Transmitter timeout
    private val staleAfterMillis = 5_000L

    // Starting of the journey
    fun startJourney(context: Context, route: TransitRoute) {
        if (state != null) {
            return
        }

        val firstStop = route.stops.firstOrNull()

        // State of journey at a given time
        state = JourneyState(
            route = route,
            currentStopIndex = 0,
            // Check journey's phase
            phase = if (firstStop?.hasAuracast == true) {
                JourneyPhase.SEARCHING
            } else {
                JourneyPhase.AT_UNCOVERED
            },
            deviceAddress = null,
            phaseStartedAt = System.currentTimeMillis()
        )

        // PROBE - EXPERIMENTAL
        BroadcastAssistantProbe.probe(context.applicationContext)

        // Create BLE scanner
        val newScanner = BroadcastScanner(
            context = context.applicationContext,
            onBroadcastFound = { result, metadata ->
                onScanResult(
                    address = result.device.address,
                    name = result.scanRecord?.deviceName,
                    rssi = result.rssi,
                    metadata = metadata
                )
            }
        )

        // Start + update scanner
        newScanner.start()
        scanner = newScanner
    }

    // Complete journey, reset vars
    fun endJourney() {
        leaveBroadcast()
        scanner?.stop()
        scanner = null
        visible.clear()
        state = null
    }

    // Scanner's bg thread
    private fun onScanResult(
        address: String,
        name: String?,
        rssi: Int,
        metadata: BroadcastMetadata
    ) {
        val now = System.currentTimeMillis()

        visible[address] = DiscoveredBroadcast(
            deviceAddress = address,
            broadcastName = name,
            rssi = rssi,
            metadata = metadata,
            lastSeenMillis = now
        )

        // If a transmitter has left, its reports stop
        visible.entries.removeAll { now - it.value.lastSeenMillis > staleAfterMillis }

        val current = state ?: return
        val decision = MatchingEngine.decide(current, visible.values.toList(), now)

        apply(decision, now)
    }

    // Act of the machine engine's decision
    private fun apply(decision: MatchDecision, now: Long) {
        val current = state ?: return

        when (decision) {
            is MatchDecision.Connect -> {
                joinBroadcast(decision.broadcast)
                state = current.copy(
                    deviceAddress = decision.broadcast.deviceAddress,
                    phase = JourneyPhase.RECEIVING,
                    phaseStartedAt = now
                )
            }

            MatchDecision.Disconnect -> {
                leaveBroadcast()
                state = current.copy(
                    deviceAddress = null,
                    phase = JourneyPhase.SEARCHING,
                    phaseStartedAt = now
                )
            }

            MatchDecision.Advance -> {
                leaveBroadcast()

                val nextIndex = current.currentStopIndex + 1
                val nextStop = current.route.stops.getOrNull(nextIndex)

                state = current.copy(
                    currentStopIndex = nextIndex,
                    deviceAddress = null,
                    phase = if (nextStop?.hasAuracast == true) {
                        JourneyPhase.SEARCHING
                    } else {
                        JourneyPhase.AT_UNCOVERED
                    },
                    phaseStartedAt = now
                )
            }

            MatchDecision.Fault -> {
                state = current.copy(
                    phase = JourneyPhase.DROP_OUT,
                    phaseStartedAt = now
                )
            }

            MatchDecision.DoNothing -> {
                // Nothing
            }
        }
    }

    // TODO: PRIVILIEGED ROOT
    private fun joinBroadcast(broadcast: DiscoveredBroadcast) {
        Log.i("JourneyViewModel", "JOIN requested: ${broadcast.deviceAddress} " +
                "(stop ${broadcast.metadata.stopIndex}, rssi ${broadcast.rssi})")

        // The actual addSource() call needs a BluetoothLeBroadcastMetadata,
        // which can't be built from a raw scan result — the assistant's own
        // startSearchingForSources() supplies it. Both are privileged APIs.
        // See BroadcastAssistantProbe for whether that route is open at all.
    }

    private fun leaveBroadcast() {
        val address = state?.deviceAddress ?: return
        Log.i("JourneyViewModel", "LEAVE requested: $address")
    }

    override fun onCleared() {
        endJourney()
    }
}