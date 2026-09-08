package com.waslni.driver.core.maps

/**
 * Exception thrown by [RoutingEngine] when route calculation fails.
 *
 * Subtypes map to UI states:
 *   - [NoRouteFound]        → "لا يوجد طريق إلى هذا الموقع"
 *   - [RoutingNetworkError] → "تعذر الاتصال بخدمة الملاحة"
 *   - [RoutingError]        → generic fallback
 */
sealed class RoutingException(message: String, cause: Throwable? = null) : Exception(message, cause)

class NoRouteFound(message: String = "No route found between the given points") :
    RoutingException(message)

class RoutingNetworkError(cause: Throwable) :
    RoutingException("Network error during route calculation", cause)

class RoutingError(message: String, cause: Throwable? = null) :
    RoutingException(message, cause)
