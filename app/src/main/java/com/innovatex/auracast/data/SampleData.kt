package com.innovatex.auracast.data

object SampleData {

    private val route86Outbound = listOf(
        Stop("s8", "Parliament", "Stop 8 · Spring St", "86-08"),
        Stop("s10", "Nicholson Street", "Stop 10 · Gertrude St", null),
        Stop("s12", "Gertrude Street", "Stop 12 · Smith St", "86-12"),
        Stop("s13", "Langridge Street", "Stop 13 · Smith St", null),
        Stop("s15", "Johnston Street", "Stop 15 · Smith St", "86-15"),
        Stop("s17", "Leicester Street", "Stop 17 · Smith St", null),
        Stop("s20", "Westgarth Street", "Stop 20 · High St", "86-20")
    )

    val routes = listOf(
        TransitRoute("86-out", "86", "To Bundoora RMIT", route86Outbound),
        TransitRoute("86-in", "86", "To Waterfront City", route86Outbound.reversed()),
        TransitRoute("96-out", "96", "To Brunswick East", emptyList())
    )
}