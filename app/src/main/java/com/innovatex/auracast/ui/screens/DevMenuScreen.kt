package com.innovatex.auracast.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.ui.navigation.Accessibility
import com.innovatex.auracast.ui.navigation.Arrived
import com.innovatex.auracast.ui.navigation.Home
import com.innovatex.auracast.ui.navigation.Journey
import com.innovatex.auracast.ui.navigation.RouteConfirm
import com.innovatex.auracast.ui.navigation.RouteSelect
import com.innovatex.auracast.ui.navigation.SetupCheck

@Composable
fun DevMenuScreen(
    modifier: Modifier = Modifier,
    onGo: (Any) -> Unit = {}
) {
    val destinations: List<Pair<String, Any>> = listOf(
        "01 · Home" to Home,
        "02 · Setup check" to SetupCheck,
        "03 · Route select" to RouteSelect,
        "04 · Confirm journey" to RouteConfirm("86-out"),
        "05 · Journey — searching" to Journey("86-out", 2, "SEARCHING"),
        "06 · Journey — receiving" to Journey("86-out", 2, "RECEIVING"),
        "06b · Journey — no coverage" to Journey("86-out", 3, "AT_UNCOVERED"),
        "07 · Journey — travelling" to Journey("86-out", 2, "TRAVELLING"),
        "08 · Arrived" to Arrived,
        "10 · Accessibility" to Accessibility
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Dev menu",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))

        destinations.forEach { (label, route) ->
            OutlinedButton(
                onClick = { onGo(route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}