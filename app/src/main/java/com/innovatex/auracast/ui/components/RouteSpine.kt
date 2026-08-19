package com.innovatex.auracast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.data.Stop
import com.innovatex.auracast.ui.theme.Ink2
import com.innovatex.auracast.ui.theme.Muted
import com.innovatex.auracast.ui.theme.ReceivingGreen
import com.innovatex.auracast.ui.theme.RouteLine
import com.innovatex.auracast.ui.theme.SignalAmber

enum class StopState {
    PASSED,
    UPCOMING,
    SEARCHING,
    RECEIVING,
    AT_UNCOVERED
}

@Composable
fun RouteSpine(
    stops: List<Stop>,
    modifier: Modifier = Modifier,
    stateFor: (Stop) -> StopState = { StopState.UPCOMING }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        stops.forEachIndexed { index, stop ->
            StopRow(
                stop = stop,
                state = stateFor(stop),
                isLast = index == stops.lastIndex
            )
        }
    }
}

@Composable
private fun StopRow(
    stop: Stop,
    state: StopState,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isCurrent = state == StopState.SEARCHING ||
            state == StopState.RECEIVING ||
            state == StopState.AT_UNCOVERED

    Row(modifier = modifier.fillMaxWidth()) {

        Column(
            modifier = Modifier.width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StopNode(stop = stop, state = state)
            if (!isLast) {
                Spacer(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(3.dp)
                        .height(if (isCurrent) 28.dp else 22.dp)
                        .background(
                            if (state == StopState.PASSED) Ink2 else RouteLine
                        )
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                text = stop.name,
                style = if (isCurrent) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = when (state) {
                    StopState.PASSED -> Muted
                    StopState.UPCOMING -> if (stop.hasAuracast) Ink2 else Muted
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Spacer(Modifier.height(2.dp))
            StopSubtitle(stop = stop, state = state)
        }
    }
}

@Composable
private fun StopNode(stop: Stop, state: StopState) {
    when (state) {
        StopState.RECEIVING -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(ReceivingGreen)
        )

        StopState.SEARCHING -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(SignalAmber)
        )

        StopState.AT_UNCOVERED -> Box(
            modifier = Modifier
                .size(16.dp)
                .border(2.dp, Muted, CircleShape)
        )

        StopState.PASSED -> Box(
            modifier = Modifier
                .size(if (stop.hasAuracast) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(RouteLine)
        )

        StopState.UPCOMING -> if (stop.hasAuracast) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(3.dp, RouteLine, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .border(2.dp, Muted.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}

@Composable
private fun StopSubtitle(stop: Stop, state: StopState) {
    when {
        state == StopState.RECEIVING -> Pill(
            text = "LIVE",
            textColor = ReceivingGreen,
            background = ReceivingGreen.copy(alpha = 0.13f)
        )

        state == StopState.PASSED -> Text(
            text = if (stop.hasAuracast) "Passed · received" else "Passed · no announcements",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )

        !stop.hasAuracast -> Pill(
            text = "NO ANNOUNCEMENTS",
            textColor = Muted,
            background = Color.Transparent,
            borderColor = RouteLine
        )

        else -> Text(
            text = stop.stopLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
    }
}

@Composable
private fun Pill(
    text: String,
    textColor: Color,
    background: Color,
    borderColor: Color? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun RouteBadgeRow(
    routeNumber: String,
    destination: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.innovatex.auracast.ui.screens.RouteBadge(routeNumber)
        Spacer(Modifier.width(12.dp))
        Text(
            text = destination,
            style = MaterialTheme.typography.titleMedium
        )
    }
}