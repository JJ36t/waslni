package com.waslni.driver.domain.usecase.notification

import com.waslni.driver.core.notifications.NotificationHelper
import com.waslni.driver.core.notifications.NotificationType
import javax.inject.Inject

/**
 * Sends a local notification of the given [type].
 *
 * Thin wrapper around [NotificationHelper] — kept as a use case so the
 * domain layer can trigger notifications without knowing about Android's
 * NotificationManager.
 *
 * Returns the notification ID, or -1 if permission denied / send failed.
 */
class SendNotificationUseCase @Inject constructor(
    private val helper: NotificationHelper
) {
    operator fun invoke(
        type: NotificationType,
        title: String? = null,
        message: String? = null
    ): Int = helper.notify(type = type, title = title, message = message)
}

/**
 * Cancels a previously sent notification by ID.
 */
class CancelNotificationUseCase @Inject constructor(
    private val helper: NotificationHelper
) {
    operator fun invoke(notificationId: Int) {
        helper.cancel(notificationId)
    }
}

/**
 * Checks whether the app has POST_NOTIFICATIONS permission.
 *
 * Used by the UI to decide whether to show a "enable notifications" prompt.
 */
class HasNotificationPermissionUseCase @Inject constructor(
    private val helper: NotificationHelper
) {
    operator fun invoke(): Boolean = helper.hasNotificationPermission()
}
