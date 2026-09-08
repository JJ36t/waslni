package com.waslni.driver.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.waslni.driver.R

/**
 * Centralized route constants for Navigation Compose.
 *
 * Why constants instead of hardcoded strings:
 * - Avoid typos that cause silent navigation failures.
 * - Refactor-safe (IDE can find usages).
 * - Single source of truth for route paths.
 *
 * Conventions:
 * - Routes use lower_snake_case.
 * - Arguments are declared as `{argName}` in the route template.
 * - Each screen section has its own object.
 */
object Routes {

    // === Auth ===
    const val SPLASH = "splash"
    const val LOGIN = "login"

    // === Main app (bottom-nav destinations) ===
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    // === Customers flow ===
    const val ADD_CUSTOMER = "customers/add"
    const val EDIT_CUSTOMER = "customers/edit/{customerId}"
    const val CUSTOMER_DETAILS = "customers/{customerId}"

    // === Delivery flow ===
    const val ACTIVE_DELIVERY = "delivery/active/{deliveryId}"
    const val NAVIGATION = "navigation/{deliveryId}"

    fun activeDelivery(deliveryId: String) = "delivery/active/$deliveryId"
    fun navigation(deliveryId: String) = "navigation/$deliveryId"

    // === Location flow ===
    const val CAPTURE_LOCATION = "location/capture"

    /**
     * Helper to build a route with arguments.
     * Usage: Routes.customerDetails("uuid-123")
     */
    fun customerDetails(customerId: String) = "customers/$customerId"
    fun editCustomer(customerId: String) = "customers/edit/$customerId"
    fun activeDelivery(deliveryId: String) = "delivery/active/$deliveryId"
    fun navigation(deliveryId: String) = "navigation/$deliveryId"

    /**
     * Bottom navigation destinations in display order (RTL-aware — the framework
     * will mirror them automatically based on LayoutDirection).
     */
    val bottomNavDestinations = listOf(
        BottomNavDestination.HOME,
        BottomNavDestination.CUSTOMERS,
        BottomNavDestination.HISTORY,
        BottomNavDestination.SETTINGS
    )
}

/**
 * Metadata for bottom navigation items.
 *
 * @param route  Route constant from [Routes].
 * @param labelResId  String resource ID for the localized label.
 * @param icon  Unselected icon.
 * @param selectedIcon  Selected icon (can be the same as `icon`).
 */
enum class BottomNavDestination(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(
        route = Routes.HOME,
        labelResId = R.string.nav_home,
        icon = Icons.AutoMirrored.Filled.Map,
        selectedIcon = Icons.AutoMirrored.Filled.Map
    ),
    CUSTOMERS(
        route = Routes.CUSTOMERS,
        labelResId = R.string.nav_customers,
        icon = Icons.Filled.People,
        selectedIcon = Icons.Filled.People
    ),
    HISTORY(
        route = Routes.HISTORY,
        labelResId = R.string.nav_history,
        icon = Icons.AutoMirrored.Filled.List,
        selectedIcon = Icons.AutoMirrored.Filled.List
    ),
    SETTINGS(
        route = Routes.SETTINGS,
        labelResId = R.string.nav_settings,
        icon = Icons.Filled.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}
