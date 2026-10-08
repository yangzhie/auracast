package com.innovatex.auracast.ui.screens

import androidx.activity.compose.BackHandler
import com.innovatex.auracast.R
import com.innovatex.auracast.ui.i18n.appString
import com.innovatex.auracast.ui.i18n.localizedRouteDestination
import com.innovatex.auracast.ui.i18n.localizedStopName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovatex.auracast.bluetooth.JourneyViewModel
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.ui.components.ConnectionFeedbackHost
import com.innovatex.auracast.ui.theme.AppearanceMode
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings


@Composable
fun JourneyRoute(
    route: TransitRoute,
    modifier: Modifier = Modifier,
    onEndJourney: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {},
    onPlanAnotherJourney: () -> Unit = {},
    audioPlaybackConfirmed: Boolean = false
) {
    val context = LocalContext.current
    val viewModel: JourneyViewModel = viewModel()

    LaunchedEffect(route.id) {
        viewModel.startJourney(context, route)
    }

    val state = viewModel.state
    val accessibility = LocalAccessibilitySettings.current
    val view = LocalView.current

    // Only hold the screen awake while this journey screen is in composition.
    DisposableEffect(view, accessibility.keepScreenOn) {
        val wasKeepingScreenOn = view.keepScreenOn
        if (accessibility.keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = wasKeepingScreenOn
        }
    }

    LaunchedEffect(state?.isJourneyOver) {
        if (state?.isJourneyOver == true) {
            viewModel.endJourney()
            onEndJourney()
        }
    }

    // Notify only when another receiver/audio integration confirms playback.
    val connectionEventKey = if (
        state != null &&
        state.phase == JourneyPhase.RECEIVING &&
        audioPlaybackConfirmed &&
        state.deviceAddress != null
    ) {
        "${state.deviceAddress}:${state.phaseStartedAt}"
    } else {
        null
    }

    ConnectionFeedbackHost(eventKey = connectionEventKey, modifier = modifier.fillMaxSize()) {
        if (state == null || state.isJourneyOver) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(appString(R.string.preparing_journey), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            JourneyScreen(
                state = state,
                audioPlaybackConfirmed = audioPlaybackConfirmed,
                onOpenAccessibility = onOpenAccessibility,
                onEndJourney = {
                    viewModel.endJourney()
                    onEndJourney()
                },
                onPlanAnotherJourney = {
                    viewModel.endJourney()
                    onPlanAnotherJourney()
                }
            )
        }
    }
}

@Composable
fun JourneyScreen(
    state: JourneyState,
    modifier: Modifier = Modifier,
    onEndJourney: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {},
    onPlanAnotherJourney: () -> Unit = {},
    audioPlaybackConfirmed: Boolean = false
) {
    val route = state.route
    val currentIndex = state.currentStopIndex.coerceIn(
        0,
        (route.stops.size - 1).coerceAtLeast(0)
    )
    val currentStop = state.currentTargetStop
    val stopName = if (currentStop != null) {
        localizedStopName(currentStop)
    } else {
        appString(R.string.generic_this_stop)
    }
    val scanStopName = if (currentStop != null) {
        localizedStopName(currentStop)
    } else {
        appString(R.string.generic_your_stop)
    }
    val phase = state.phase.name
    val isColourful = LocalAccessibilitySettings.current.appearance == AppearanceMode.COLOURFUL
    val colors = MaterialTheme.colorScheme
    val statusPanelColor = if (isColourful) {
        if (phase == "RECEIVING" && audioPlaybackConfirmed) {
            colors.secondaryContainer
        } else if (phase == "DROP_OUT") {
            colors.tertiaryContainer
        } else if (phase == "SEARCHING" || phase == "CONNECTING") {
            colors.primaryContainer
        } else {
            colors.surfaceVariant
        }
    } else {
        colors.primaryContainer
    }
    val audioPanelColor = if (isColourful) {
        colors.tertiaryContainer
    } else {
        colors.surface
    }
    var confirmation by remember { mutableStateOf<String?>(null) }

    BackHandler {
        confirmation = "end"
    }

    if (confirmation != null) {
        val planningAnother = confirmation == "another"
        val title = if (planningAnother) {
            appString(R.string.dialog_plan_another)
        } else {
            appString(R.string.dialog_end_journey)
        }
        val confirmLabel = if (planningAnother) {
            appString(R.string.plan_another_short)
        } else {
            appString(R.string.end_journey)
        }

        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(title) },
            text = { Text(appString(R.string.dialog_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        if (planningAnother) {
                            onPlanAnotherJourney()
                        } else {
                            onEndJourney()
                        }
                    }
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text(appString(R.string.keep_travelling))
                }
            }
        )
    }

    val status = when (phase) {
        "RECEIVING" -> {
            if (audioPlaybackConfirmed) {
                Triple(
                    appString(R.string.status_receiving_audio),
                    appString(R.string.status_audio_playing),
                    appString(R.string.confirm_audio_stream)
                )
            } else {
                Triple(
                    appString(R.string.status_broadcast_detected),
                    appString(R.string.signal_found, stopName),
                    appString(R.string.audio_unconfirmed_detail)
                )
            }
        }
        "CONNECTING" -> Triple(
            appString(R.string.connecting),
            appString(R.string.connecting_to, stopName),
            appString(R.string.connecting_detail)
        )
        "SEARCHING" -> Triple(
            appString(R.string.searching),
            appString(R.string.looking_for_announcements),
            appString(R.string.scanning_near, scanStopName)
        )
        "TRAVELLING" -> Triple(
            appString(R.string.between_stops),
            appString(R.string.continuing_journey),
            appString(R.string.search_next_supported)
        )
        "AT_UNCOVERED" -> Triple(
            appString(R.string.no_coverage),
            appString(R.string.no_auracast_here),
            appString(R.string.continue_next_supported)
        )
        "DROP_OUT" -> Triple(
            appString(R.string.connection_interrupted),
            appString(R.string.broadcast_unavailable),
            appString(R.string.check_receiver)
        )
        else -> Triple(
            appString(R.string.journey_in_progress),
            appString(R.string.checking_connection),
            appString(R.string.updating_status)
        )
    }

    val audioStatus = if (phase == "RECEIVING" && audioPlaybackConfirmed) {
        appString(R.string.playback_confirmed)
    } else {
        appString(R.string.playback_not_confirmed)
    }
    val stopNumber = if (route.stops.isEmpty()) {
        0
    } else {
        currentIndex + 1
    }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        route.routeNumber,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(appString(R.string.live_journey), style = MaterialTheme.typography.labelLarge)
                    Text(
                        localizedRouteDestination(route),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onOpenAccessibility,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.AccessibilityNew,
                        contentDescription = appString(R.string.open_accessibility)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = statusPanelColor
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().semantics {
                    liveRegion = LiveRegionMode.Polite
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null)
                        Text(
                            status.first,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        status.second,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(status.third, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = audioPanelColor
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Default.Hearing,
                        contentDescription = null,
                        tint = if (isColourful) {
                            colors.tertiary
                        } else {
                            colors.onSurface
                        },
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appString(R.string.auracast_audio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            audioStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Tram, contentDescription = null)
                Column {
                    Text(
                        appString(R.string.your_stops),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        appString(R.string.stop_count, stopNumber, route.stops.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    route.stops.forEachIndexed { stopIndex, stop ->
                        val stateLabel = if (stopIndex < state.currentStopIndex) {
                            appString(R.string.passed)
                        } else if (stopIndex == state.currentStopIndex) {
                            appString(R.string.current_stop)
                        } else {
                            appString(R.string.upcoming)
                        }
                        val coverageLabel = if (stop.hasAuracast) {
                            appString(R.string.auracast_supported)
                        } else {
                            appString(R.string.no_auracast_coverage)
                        }
                        val localizedName = localizedStopName(stop)
                        val accessibleStopDescription = appString(
                            R.string.stop_state_description,
                            localizedName,
                            stateLabel,
                            coverageLabel
                        )
                        val stopIcon = if (stopIndex <= state.currentStopIndex) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Radio
                        }
                        val textWeight = if (stopIndex == state.currentStopIndex) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = accessibleStopDescription
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    stopIcon,
                                    contentDescription = null,
                                    tint = if (isColourful && stopIndex == state.currentStopIndex) {
                                        colors.secondary
                                    } else if (isColourful && stopIndex > state.currentStopIndex) {
                                        colors.primary
                                    } else {
                                        colors.onSurface
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    localizedName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = textWeight
                                )
                                Text(
                                    appString(R.string.stop_state_line, stateLabel, coverageLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (stopIndex != route.stops.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { confirmation = "another" },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(appString(R.string.plan_another_journey))
            }
            Button(
                onClick = { confirmation = "end" },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(appString(R.string.end_journey))
            }
        }
    }
}
