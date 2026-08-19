package com.innovatex.auracast.data

data class JourneySummary(
    val destinationName: String,
    val coveredStopsConnected: Int,
    val coveredStopsTotal: Int,
    val uncoveredStopsPassed: Int,
    val journeyMinutes: Int
) {
    val allCoveredStopsConnected: Boolean
        get() = coveredStopsConnected == coveredStopsTotal
}