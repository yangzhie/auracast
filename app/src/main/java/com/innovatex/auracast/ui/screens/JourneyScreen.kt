package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovatex.auracast.bluetooth.JourneyViewModel
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.data.Stop
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.ui.components.BandStyles
import com.innovatex.auracast.ui.components.RouteBadgeRow
import com.innovatex.auracast.ui.components.RouteSpine
import com.innovatex.auracast.ui.components.StatusBand
import com.innovatex.auracast.ui.components.StopState
import com.innovatex.auracast.ui.theme.AlertRed

/**
 * Live journey. Owns the ViewModel, which owns the scanner and drives
 * state through MatchingEngine.
 */
@Composable
fun JourneyRoute(
    route: TransitRoute,
    modifier: Modifier = Modifier,
    onEndJourney: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: JourneyViewModel = viewModel()

    // Keyed on route id so a different route restarts the journey.
    LaunchedEffect(route.id) {
        viewModel.startJourney(context, route)
    }

    val state = viewModel.state

    if (state == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Starting journey…",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    JourneyScreen(
        state = state,
        modifier = modifier,
        onEndJourney = {
            viewModel.endJourney()
            onEndJourney()
        },
        onOpenAccessibility = onOpenAccessibility
    )
}

/**
 * Stateless. Renders whatever JourneyState it's given, so the dev menu can
 * feed it a hand-built state and the live route can feed it a real one.
 */
@Composable
fun JourneyScreen(
    state: JourneyState,
    modifier: Modifier = Modifier,
    onEndJourney: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {}
) {
    val route = state.route
    val phase = state.phase
    val currentStopIndex = state.currentStopIndex
    val nextCovered = state.nextAuracastEnabledStop

    // Null once the journey has run past the end of the route.
    val currentStop = state.currentTargetStop ?: return

    Column(modifier = modifier.fillMaxSize()) {

        RouteBadgeRow(
            routeNumber = route.routeNumber,
            destination = route.destination,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            trailingContent = {
                IconButton(onClick = onOpenAccessibility) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Accessibility settings"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            when (phase) {
                JourneyPhase.SEARCHING -> StatusBand(
                    kicker = "Searching",
                    headline = "Approaching\n${currentStop.name}",
                    detail = "Looking for this stop's announcements.",
                    style = BandStyles.Searching
                )

                JourneyPhase.RECEIVING -> StatusBand(
                    kicker = "Receiving",
                    headline = currentStop.name,
                    detail = "Playing to your hearing aids.",
                    style = BandStyles.Receiving
                )

                JourneyPhase.TRAVELLING -> StatusBand(
                    kicker = "Between stops",
                    headline = "On the way to\n${nextCovered?.name ?: "your destination"}",
                    detail = "You'll connect automatically when you arrive.",
                    style = BandStyles.Travelling
                )

                JourneyPhase.AT_UNCOVERED -> StatusBand(
                    kicker = "No announcements",
                    headline = currentStop.name,
                    detail = nextCovered?.let {
                        "This stop isn't fitted yet. Nothing is wrong — you'll reconnect at ${it.name}."
                    } ?: "This stop isn't fitted yet. Nothing is wrong.",
                    style = BandStyles.NoCoverage
                )

                JourneyPhase.DROP_OUT -> StatusBand(
                    kicker = "Not receiving",
                    headline = "Connection dropped",
                    detail = "Trying to reconnect to ${currentStop.name}.",
                    style = BandStyles.Fault
                )
            }

            Spacer(Modifier.height(20.dp))

            RouteSpine(
                stops = visibleStops(route.stops, currentStopIndex),
                stateFor = { stop ->
                    stopStateFor(stop, currentStop, route.stops, currentStopIndex, phase)
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = onEndJourney,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("End journey", color = AlertRed)
        }
    }
}

private fun visibleStops(stops: List<Stop>, currentIndex: Int): List<Stop> {
    val start = (currentIndex - 1).coerceAtLeast(0)
    val end = (currentIndex + 3).coerceAtMost(stops.size)
    return stops.subList(start, end)
}

private fun stopStateFor(
    stop: Stop,
    currentStop: Stop,
    allStops: List<Stop>,
    currentIndex: Int,
    phase: JourneyPhase
): StopState {
    val stopIndex = allStops.indexOf(stop)

    return when {
        stopIndex < currentIndex -> StopState.PASSED

        stop.id == currentStop.id -> when (phase) {
            JourneyPhase.SEARCHING -> StopState.SEARCHING
            JourneyPhase.RECEIVING -> StopState.RECEIVING
            JourneyPhase.AT_UNCOVERED -> StopState.AT_UNCOVERED
            JourneyPhase.TRAVELLING -> StopState.PASSED
            JourneyPhase.DROP_OUT -> StopState.SEARCHING
        }

        else -> StopState.UPCOMING
    }
}