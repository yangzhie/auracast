package com.innovatex.auracast.data

data class TransitRoute(
    val id: String,
    val routeNumber: String,
    val destination: String,
    val stops: List<Stop>
) {
    val coveredStopCount: Int
        get() = stops.count { it.hasAuracast }

    val totalStopCount: Int
        get() = stops.size

    val coverageSummary: String
        get() = if (coveredStopCount == 0) {
            "No stops fitted yet"
        } else {
            "$coveredStopCount of $totalStopCount stops have announcements"
        }
}