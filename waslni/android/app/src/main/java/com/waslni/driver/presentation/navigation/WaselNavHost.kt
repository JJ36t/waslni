package com.waslni.driver.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.waslni.driver.presentation.auth.login.LoginScreen
import com.waslni.driver.presentation.auth.splash.SplashScreen
import com.waslni.driver.presentation.customers.CustomerDetailsScreen
import com.waslni.driver.presentation.customers.CustomerListScreen
import com.waslni.driver.presentation.customers.AddCustomerScreen
import com.waslni.driver.presentation.customers.EditCustomerScreen
import com.waslni.driver.presentation.delivery.HistoryScreen
import com.waslni.driver.presentation.home.HomeScreen
import com.waslni.driver.presentation.settings.SettingsScreen

/**
 * Root navigation host for the app.
 *
 * Structure:
 *   Splash → (auth check) → Login OR Home
 *
 * The bottom navigation bar is only shown on the four main destinations
 * (Home, Customers, History, Settings). On detail/edit screens it is hidden.
 *
 * In Phase 11 we'll wire the splash → login/home decision to the real
 * AuthRepository (token check). For now splash navigates to login after a delay.
 */
@Composable
fun WaselNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in Routes.bottomNavDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WaselniBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            // Pop up to the start destination to avoid building a large back stack
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // === Auth flow ===
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // === Main bottom-nav destinations ===
            composable(Routes.HOME) {
                HomeScreen(
                    onAddCustomerClick = { navController.navigate(Routes.ADD_CUSTOMER) },
                    onCustomerClick = { id -> navController.navigate(Routes.customerDetails(id)) }
                )
            }
            composable(Routes.CUSTOMERS) {
                CustomerListScreen(
                    onAddCustomerClick = { navController.navigate(Routes.ADD_CUSTOMER) },
                    onCustomerClick = { id -> navController.navigate(Routes.customerDetails(id)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }

            // === Customer flow ===
            composable(Routes.ADD_CUSTOMER) {
                AddCustomerScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CUSTOMER_DETAILS,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: return@composable
                CustomerDetailsScreen(
                    customerId = customerId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editCustomer(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EDIT_CUSTOMER,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: return@composable
                EditCustomerScreen(
                    customerId = customerId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // === Location flow (Phase 4 — standalone capture screen) ===
            composable(Routes.CAPTURE_LOCATION) {
                com.waslni.driver.presentation.location.LocationPermissionGate(
                    onPermissionGranted = { /* proceed */ },
                    onPermissionDenied = { navController.popBackStack() }
                ) {
                    com.waslni.driver.presentation.location.CaptureLocationScreen(
                        onBack = { navController.popBackStack() },
                        onConfirm = { _ ->
                            // Phase 4: just pop back — Phase 6 will pass the
                            // captured location back to the Add Customer form.
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation bar.
 *
 * Items are mirrored automatically based on LayoutDirection (RTL/LTR).
 */
@Composable
private fun WaselniBottomBar(
    currentRoute: String?,
    onNavigate: (BottomNavDestination) -> Unit
) {
    NavigationBar {
        Routes.bottomNavDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(destination.labelResId)) },
                alwaysShowLabel = true
            )
        }
    }
}
