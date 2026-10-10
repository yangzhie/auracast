package com.innovatex.auracast.data

object SampleData {

    /*
     * Demo route: four fitted stops, modelled with nothing between them.
     * The real 86 has unfitted stops in between, but the app can only
     * observe the rider where a transmitter is present.
     */
    private val route86Outbound = listOf(
        Stop("s8", "Parliament", "Stop 8 · Spring St", BroadcastIdentity(86, 1)),
        Stop("s12", "Gertrude Street", "Stop 12 · Smith St", BroadcastIdentity(86, 2)),
        Stop("s15", "Johnston Street", "Stop 15 · Smith St", BroadcastIdentity(86, 3)),
        Stop("s20", "Westgarth Street", "Stop 20 · High St", BroadcastIdentity(86, 4))
    )

    val routes = listOf(
        TransitRoute("86-out", "86", "To Bundoora RMIT", route86Outbound),

        // Kept so the route screen can show the "no stops fitted yet" case.
        TransitRoute("96-out", "96", "To Brunswick East", emptyList())
    )

    fun routeById(id: String): TransitRoute = routes.first { it.id == id }
}