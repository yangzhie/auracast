package com.innovatex.auracast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.ui.screens.AccessibilityScreen
import com.innovatex.auracast.ui.screens.ArrivedScreen
import com.innovatex.auracast.ui.screens.HomeScreen
import com.innovatex.auracast.ui.screens.JourneyScreen
import com.innovatex.auracast.ui.screens.RouteConfirmScreen
import com.innovatex.auracast.ui.screens.RouteScreen
import com.innovatex.auracast.ui.screens.SetupCheckScreen
import com.innovatex.auracast.ui.screens.JourneyPhase
import com.innovatex.auracast.ui.screens.DevMenuScreen
import kotlinx.serialization.Serializable

@Serializable object Home

@Serializable object SetupCheck

@Serializable object RouteSelect

@Serializable data class RouteConfirm(val routeId: String)

@Serializable object Arrived

@Serializable object Accessibility

@Serializable
data class Journey(
    val routeId: String,
    val stopIndex: Int = 2,
    val phaseName: String = JourneyPhase.RECEIVING.name
)

@Serializable object DevMenu

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = DevMenu,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onPlanJourney = { navController.navigate(SetupCheck) }
            )
        }

        composable<SetupCheck> {
            SetupCheckScreen(
                onContinue = { navController.navigate(RouteSelect) }
            )
        }

        composable<RouteSelect> {
            RouteScreen(
                onContinue = { route ->
                    navController.navigate(RouteConfirm(route.id))
                }
            )
        }

        composable<RouteConfirm> { backStackEntry ->
            val args = backStackEntry.toRoute<RouteConfirm>()
            RouteConfirmScreen(
                route = SampleData.routeById(args.routeId),
                onStartJourney = { navController.navigate(Journey(args.routeId)) }
            )
        }

        composable<Journey> { backStackEntry ->
            val args = backStackEntry.toRoute<Journey>()
            JourneyScreen(
                route = SampleData.routeById(args.routeId),
                currentStopIndex = args.stopIndex,
                phase = JourneyPhase.valueOf(args.phaseName),
                onEndJourney = {
                    navController.navigate(Arrived) { popUpTo<Home>() }
                },
                onOpenAccessibility = { navController.navigate(Accessibility) }
            )
        }

        composable<Arrived> {
            ArrivedScreen(
                onPlanAnother = {
                    navController.navigate(Home) {
                        popUpTo<Home>{ inclusive = true }
                    }
                }
            )
        }

        composable<Accessibility> {
            AccessibilityScreen(
                onDone = { navController.popBackStack() }
            )
        }

        composable<DevMenu> {
            DevMenuScreen(onGo = { navController.navigate(it) })
        }
    }
}