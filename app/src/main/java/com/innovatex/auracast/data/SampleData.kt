package com.innovatex.auracast.data

object SampleData {
    private val route86Outbound = listOf(
        Stop("s8", "Parliament", "Stop 8 · Spring St", BroadcastIdentity(86, 1)),
        Stop("s10", "Nicholson Street", "Stop 10 · Gertrude St", null),
        Stop("s12", "Gertrude Street", "Stop 12 · Smith St", BroadcastIdentity(86, 2)),
        Stop("s13", "Langridge Street", "Stop 13 · Smith St", null),
        Stop("s15", "Johnston Street", "Stop 15 · Smith St", BroadcastIdentity(86, 3)),
        Stop("s17", "Leicester Street", "Stop 17 · Smith St", null),
        Stop("s20", "Westgarth Street", "Stop 20 · High St", BroadcastIdentity(86, 4))
    )

    val routes = listOf(
        TransitRoute("86-out", "86", "To Bundoora RMIT", route86Outbound),

        // Kept so the route screen can show the "no stops fitted yet" case.
        TransitRoute("96-out", "96", "To Brunswick East", emptyList())
    )

    fun routeById(id: String): TransitRoute = routes.first { it.id == id }
}