package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.data.JourneySummary
import com.innovatex.auracast.ui.theme.Muted

@Composable
fun ArrivedScreen(
    modifier: Modifier = Modifier,
    summary: JourneySummary = SampleSummary,
    onPlanAnother: () -> Unit = {},
    onReportProblem: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
            ) {
                Text(
                    text = "You've arrived at ${summary.destinationName}.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Your hearing device has been released and is back to normal.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
            }

            Spacer(Modifier.height(28.dp))

            HorizontalDivider()

            SummaryRow(
                label = "Stops with announcements",
                value = "${summary.coveredStopsConnected} of ${summary.coveredStopsTotal}"
            )
            HorizontalDivider()
            SummaryRow(
                label = "Stops not yet fitted",
                value = summary.uncoveredStopsPassed.toString()
            )
            HorizontalDivider()
            SummaryRow(
                label = "Journey time",
                value = "${summary.journeyMinutes} min"
            )
            HorizontalDivider()
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Button(
                onClick = onPlanAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Plan another journey")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onReportProblem,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Report a problem")
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private val SampleSummary = JourneySummary(
    destinationName = "Westgarth Street",
    coveredStopsConnected = 4,
    coveredStopsTotal = 4,
    uncoveredStopsPassed = 3,
    journeyMinutes = 18
)