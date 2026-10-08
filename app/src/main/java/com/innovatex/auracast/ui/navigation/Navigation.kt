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
import com.innovatex.auracast.ui.screens.JourneyRoute
import com.innovatex.auracast.ui.screens.RouteScreen
import com.innovatex.auracast.ui.screens.SetupCheckScreen
import com.innovatex.auracast.core.JourneyPhase
import kotlinx.serialization.Serializable

@Serializable object Home
@Serializable object SetupCheck
@Serializable object RouteSelect
@Serializable data class Journey(val routeId: String)
@Serializable object Arrived
@Serializable object Accessibility

// Retained as route symbols for older developer-screen source compatibility.
// No developer/debug screens are registered in the passenger NavHost.
@Serializable data class RouteConfirm(val routeId: String)
@Serializable object ScanDebug
@Serializable object DevMenu
@Serializable data class JourneyPreview(
    val routeId: String,
    val stopIndex: Int = 2,
    val phaseName: String = JourneyPhase.RECEIVING.name
)

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onPlanJourney = { navController.navigate(SetupCheck) },
                onOpenAccessibility = { navController.navigate(Accessibility) }
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
                    // Selection immediately starts the real JourneyRoute.
                    navController.navigate(Journey(route.id))
                }
            )
        }
        composable<Journey> { backStackEntry ->
            val selected = backStackEntry.toRoute<Journey>()
            JourneyRoute(
                route = SampleData.routeById(selected.routeId),
                onEndJourney = {
                    navController.navigate(Arrived) {
                        popUpTo<Home>()
                        launchSingleTop = true
                    }
                },
                onPlanAnotherJourney = {
                    navController.navigate(SetupCheck) {
                        popUpTo<Home>()
                        launchSingleTop = true
                    }
                },
                onOpenAccessibility = { navController.navigate(Accessibility) }
            )
        }
        composable<Arrived> {
            ArrivedScreen(
                onPlanAnother = {
                    navController.navigate(SetupCheck) {
                        popUpTo<Home>()
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Accessibility> {
            AccessibilityScreen(onDone = { navController.popBackStack() })
        }
    }
}
