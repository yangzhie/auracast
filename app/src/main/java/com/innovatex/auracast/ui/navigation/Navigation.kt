package com.innovatex.auracast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.innovatex.auracast.core.JourneyPhase
import com.innovatex.auracast.core.JourneyState
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.ui.screens.AccessibilityScreen
import com.innovatex.auracast.ui.screens.ArrivedScreen
import com.innovatex.auracast.ui.screens.DevMenuScreen
import com.innovatex.auracast.ui.screens.HomeScreen
import com.innovatex.auracast.ui.screens.JourneyRoute
import com.innovatex.auracast.ui.screens.JourneyScreen
import com.innovatex.auracast.ui.screens.RouteConfirmScreen
import com.innovatex.auracast.ui.screens.RouteScreen
import com.innovatex.auracast.ui.screens.ScanDebugScreen
import com.innovatex.auracast.ui.screens.SetupCheckScreen
import kotlinx.serialization.Serializable

@Serializable object Home

@Serializable object SetupCheck

@Serializable object RouteSelect

@Serializable data class RouteConfirm(val routeId: String)

@Serializable object Arrived

@Serializable object Accessibility

@Serializable object ScanDebug

@Serializable object DevMenu

/** The real journey — scanner running, state driven by MatchingEngine. */
@Serializable data class Journey(val routeId: String)

/** Dev only: renders a hand-built state so a phase can be inspected. */
@Serializable
data class JourneyPreview(
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
            JourneyRoute(
                route = SampleData.routeById(args.routeId),
                onEndJourney = {
                    navController.navigate(Arrived) { popUpTo<Home>() }
                },
                onOpenAccessibility = { navController.navigate(Accessibility) }
            )
        }

        composable<JourneyPreview> { backStackEntry ->
            val args = backStackEntry.toRoute<JourneyPreview>()
            JourneyScreen(
                state = JourneyState(
                    route = SampleData.routeById(args.routeId),
                    currentStopIndex = args.stopIndex,
                    phase = JourneyPhase.valueOf(args.phaseName),
                    deviceAddress = null,
                    phaseStartedAt = 0L
                )
            )
        }

        composable<Arrived> {
            ArrivedScreen(
                onPlanAnother = {
                    navController.navigate(Home) {
                        popUpTo<Home> { inclusive = true }
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

        composable<ScanDebug> {
            ScanDebugScreen()
        }
    }
}