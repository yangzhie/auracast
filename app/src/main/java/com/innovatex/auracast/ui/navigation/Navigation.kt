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
import com.innovatex.auracast.ui.screens.FmaDebugScreen
import com.innovatex.auracast.ui.screens.HomeScreen
import com.innovatex.auracast.ui.screens.JourneyRoute
import com.innovatex.auracast.ui.screens.JourneyScreen
import com.innovatex.auracast.ui.screens.RouteConfirmScreen
import com.innovatex.auracast.ui.screens.RouteScreen
import com.innovatex.auracast.ui.screens.ScanDebugScreen
import com.innovatex.auracast.ui.screens.SetupCheckScreen
import kotlinx.serialization.Serializable


// =========================================================
// NAVIGATION DESTINATIONS
// =========================================================

@Serializable
object Home


@Serializable
object SetupCheck


@Serializable
object RouteSelect


@Serializable
data class RouteConfirm(
    val routeId: String
)


@Serializable
object Arrived


@Serializable
object Accessibility


/*
 * Manual FMA / Auracast debug screen.
 *
 * This is the old screen where you can manually
 * scan and select a transmitter.
 */
@Serializable
object ScanDebug


/*
 * Automatic Auracast test screen.
 *
 * Opening this screen starts:
 *
 * detect
 *      ↓
 * connect automatically
 *      ↓
 * receive
 *      ↓
 * audio relay
 */
@Serializable
object FmaDebug


/*
 * Previous developer menu.
 *
 * We keep this as the application's start screen
 * so the previous UI remains available.
 */
@Serializable
object DevMenu


@Serializable
data class Journey(
    val routeId: String
)


@Serializable
data class JourneyPreview(
    val routeId: String,
    val stopIndex: Int = 2,
    val phaseName: String =
        JourneyPhase.RECEIVING.name
)


// =========================================================
// NAVIGATION
// =========================================================

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navController: NavHostController =
        rememberNavController()
) {

    NavHost(
        navController =
            navController,

        /*
         * IMPORTANT:
         *
         * Keep the previous UI.
         *
         * Do NOT use:
         *
         * startDestination = FmaDebug
         *
         * because that bypasses the previous menu.
         */
        startDestination =
            DevMenu,

        modifier =
            modifier
    ) {


        // =====================================================
        // DEVELOPER MENU
        // =====================================================

        composable<DevMenu> {

            DevMenuScreen(
                onGo = { destination ->

                    navController.navigate(
                        destination
                    )
                }
            )
        }


        // =====================================================
        // HOME
        // =====================================================

        composable<Home> {

            HomeScreen(
                onPlanJourney = {

                    navController.navigate(
                        SetupCheck
                    )
                }
            )
        }


        // =====================================================
        // SETUP CHECK
        // =====================================================

        composable<SetupCheck> {

            SetupCheckScreen(
                onContinue = {

                    navController.navigate(
                        RouteSelect
                    )
                }
            )
        }


        // =====================================================
        // ROUTE SELECT
        // =====================================================

        composable<RouteSelect> {

            RouteScreen(
                onContinue = { route ->

                    navController.navigate(
                        RouteConfirm(
                            routeId =
                                route.id
                        )
                    )
                }
            )
        }


        // =====================================================
        // ROUTE CONFIRM
        // =====================================================

        composable<RouteConfirm> {
                backStackEntry ->


            val arguments =
                backStackEntry
                    .toRoute<RouteConfirm>()


            val route =
                SampleData.routeById(
                    arguments.routeId
                )


            RouteConfirmScreen(
                route =
                    route,

                onStartJourney = {

                    navController.navigate(
                        Journey(
                            routeId =
                                arguments.routeId
                        )
                    )
                }
            )
        }


        // =====================================================
        // JOURNEY
        // =====================================================

        composable<Journey> {
                backStackEntry ->


            val arguments =
                backStackEntry
                    .toRoute<Journey>()


            val route =
                SampleData.routeById(
                    arguments.routeId
                )


            JourneyRoute(
                route =
                    route,

                onEndJourney = {

                    navController.navigate(
                        Arrived
                    ) {

                        popUpTo<Home>()
                    }
                },

                onOpenAccessibility = {

                    navController.navigate(
                        Accessibility
                    )
                }
            )
        }


        // =====================================================
        // JOURNEY PREVIEW
        // =====================================================

        composable<JourneyPreview> {
                backStackEntry ->


            val arguments =
                backStackEntry
                    .toRoute<JourneyPreview>()


            val route =
                SampleData.routeById(
                    arguments.routeId
                )


            JourneyScreen(
                state =
                    JourneyState(
                        route =
                            route,

                        currentStopIndex =
                            arguments.stopIndex,

                        phase =
                            JourneyPhase.valueOf(
                                arguments.phaseName
                            ),

                        deviceAddress =
                            null,

                        phaseStartedAt =
                            0L
                    )
            )
        }


        // =====================================================
        // ARRIVED
        // =====================================================

        composable<Arrived> {

            ArrivedScreen(
                onPlanAnother = {

                    navController.navigate(
                        Home
                    ) {

                        popUpTo<Home> {

                            inclusive =
                                true
                        }
                    }
                }
            )
        }


        // =====================================================
        // ACCESSIBILITY
        // =====================================================

        composable<Accessibility> {

            AccessibilityScreen(
                onDone = {

                    navController.popBackStack()
                }
            )
        }


        // =====================================================
        // MANUAL AURACAST DEBUG
        // =====================================================

        /*
         * Keep this screen for comparison and debugging.
         *
         * This is where you can manually:
         *
         * Scan
         *      ↓
         * choose transmitter
         *      ↓
         * Receive
         */
        composable<ScanDebug> {

            ScanDebugScreen()
        }


        // =====================================================
        // AUTOMATIC AURACAST REQUIREMENT TEST
        // =====================================================

        /*
         * Navigate here from the Dev Menu when
         * testing the automatic requirements.
         *
         * FmaDebugScreen already calls:
         *
         * viewModel.startAutoMode(context)
         *
         * automatically when this screen opens.
         *
         * Therefore you do NOT need to press:
         *
         * Scan
         * Receive
         */
        composable<FmaDebug> {

            FmaDebugScreen()
        }
    }
}