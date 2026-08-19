package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.ui.components.RouteBadgeRow
import com.innovatex.auracast.ui.components.RouteSpine
import com.innovatex.auracast.ui.components.StopState
import com.innovatex.auracast.ui.theme.Muted
import com.innovatex.auracast.ui.theme.ReceivingGreen
import com.innovatex.auracast.ui.theme.RouteLine

@Composable
fun RouteConfirmScreen(
    modifier: Modifier = Modifier,
    route: TransitRoute = SampleData.routes.first(),
    onStartJourney: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {

        RouteBadgeRow(
            routeNumber = route.routeNumber,
            destination = route.destination,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            CoverageStrip(route = route)

            Spacer(Modifier.height(20.dp))

            RouteSpine(
                stops = route.stops,
                stateFor = { StopState.UPCOMING }
            )

            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onStartJourney,
            enabled = route.coveredStopCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Start journey")
        }
    }
}

@Composable
private fun CoverageStrip(route: TransitRoute, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${route.coveredStopCount} of ${route.totalStopCount} stops covered",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "You'll connect automatically at those ${route.coveredStopCount}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics { }
        ) {
            route.stops.forEach { stop ->
                if (stop.hasAuracast) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(ReceivingGreen)
                    )
                } else {
                    Box(
                        Modifier
                            .size(9.dp)
                            .border(2.dp, RouteLine, CircleShape)
                    )
                }
            }
        }
    }
}