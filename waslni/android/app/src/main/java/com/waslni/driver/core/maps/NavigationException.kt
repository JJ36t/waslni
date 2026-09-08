package com.waslni.driver.core.maps

/**
 * Exception thrown by [NavigationEngine] when starting navigation fails.
 */
sealed class NavigationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class NavigationRouteMissing(message: String = "Cannot navigate without a route") :
    NavigationException(message)

class NavigationEngineError(message: String, cause: Throwable? = null) :
    NavigationException(message, cause)
