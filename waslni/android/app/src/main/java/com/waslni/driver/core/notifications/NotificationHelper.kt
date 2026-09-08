package com.waslni.driver.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types of local notifications the app sends.
 *
 * Each type maps to a notification channel + a specific message template.
 */
enum class NotificationType {
    APPROACHING_CUSTOMER,  // "أنت قريب من موقع الزبون"
    ARRIVED_AT_CUSTOMER,   // "وصلت إلى موقع الزبون"
    DELIVERY_COMPLETED,    // "تم التسليم بنجاح"
    DELIVERY_CANCELLED     // "تم إلغاء التوصيل"
}

/**
 * Helper for creating + sending local notifications.
 *
 * Channels:
 *   - "delivery_status" (low importance) — delivery completion/cancellation.
 *   - "navigation_alerts" (high importance) — arrival + approaching alerts
 *     that the driver needs to see immediately.
 *
 * Permission:
 *   - POST_NOTIFICATIONS is required on API 33+ (Android 13+).
 *   - [hasNotificationPermission] checks it.
 *   - [notify] silently no-ops if permission isn't granted — the app still
 *     works, the driver just doesn't see notifications.
 *
 * The helper is a singleton so notification IDs can be managed centrally
 * (avoids accidental duplicate IDs across the app).
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    /**
     * Send a notification of the given [type] with optional [title] + [message].
     *
     * If title/message are null, defaults from string resources are used.
     *
     * @param type The notification category (determines channel + icon).
     * @param title Override title (null → use default for type).
     * @param message Override message (null → use default for type).
     * @param pendingIntent Optional intent to open when tapped.
     *
     * Returns the notification ID, or -1 if permission denied.
     */
    fun notify(
        type: NotificationType,
        title: String? = null,
        message: String? = null,
        pendingIntent: PendingIntent? = null
    ): Int {
        if (!hasNotificationPermission()) return -1

        val channelId = channelFor(type)
        val notificationId = idFor(type)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title ?: defaultTitleFor(type))
            .setContentText(message ?: defaultMessageFor(type))
            .setPriority(priorityFor(type))
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        // Vibration for high-priority alerts
        if (priorityFor(type) >= NotificationCompat.PRIORITY_HIGH) {
            builder.setVibrate(VIBRATION_PATTERN)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked between check + notify — silently skip
            return -1
        }

        return notificationId
    }

    /**
     * Cancel a previously sent notification by ID.
     */
    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications from this app.
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * True if POST_NOTIFICATIONS permission is granted (API 33+) or
     * always true on older APIs (no runtime permission needed).
     */
    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true  // pre-Android 13: no runtime permission needed
        }
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // === Channel setup ===

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_DELIVERY_STATUS,
                "حالة التوصيل",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات إكمال أو إلغاء التوصيل"
            },
            NotificationChannel(
                CHANNEL_NAVIGATION_ALERTS,
                "تنبيهات الملاحة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الوصول والاقتراب من الزبون"
                enableVibration(true)
            }
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channels.forEach { manager.createNotificationChannel(it) }
    }

    // === Type → channel/priority/id mapping ===

    private fun channelFor(type: NotificationType): String = when (type) {
        NotificationType.APPROACHING_CUSTOMER,
        NotificationType.ARRIVED_AT_CUSTOMER -> CHANNEL_NAVIGATION_ALERTS
        NotificationType.DELIVERY_COMPLETED,
        NotificationType.DELIVERY_CANCELLED -> CHANNEL_DELIVERY_STATUS
    }

    private fun priorityFor(type: NotificationType): Int = when (type) {
        NotificationType.APPROACHING_CUSTOMER,
        NotificationType.ARRIVED_AT_CUSTOMER -> NotificationCompat.PRIORITY_HIGH
        NotificationType.DELIVERY_COMPLETED,
        NotificationType.DELIVERY_CANCELLED -> NotificationCompat.PRIORITY_LOW
    }

    private fun idFor(type: NotificationType): Int = when (type) {
        NotificationType.APPROACHING_CUSTOMER -> NOTIF_ID_APPROACHING
        NotificationType.ARRIVED_AT_CUSTOMER -> NOTIF_ID_ARRIVED
        NotificationType.DELIVERY_COMPLETED -> NOTIF_ID_COMPLETED
        NotificationType.DELIVERY_CANCELLED -> NOTIF_ID_CANCELLED
    }

    private fun defaultTitleFor(type: NotificationType): String = when (type) {
        NotificationType.APPROACHING_CUSTOMER -> "اقتراب من الزبون"
        NotificationType.ARRIVED_AT_CUSTOMER -> "وصلت إلى الزبون"
        NotificationType.DELIVERY_COMPLETED -> "تم التسليم"
        NotificationType.DELIVERY_CANCELLED -> "تم الإلغاء"
    }

    private fun defaultMessageFor(type: NotificationType): String = when (type) {
        NotificationType.APPROACHING_CUSTOMER -> "أنت على بعد 100 متر من موقع الزبون."
        NotificationType.ARRIVED_AT_CUSTOMER -> "وصلت إلى موقع الزبون. اضغط \"وصلت إلى الزبون\" للتأكيد."
        NotificationType.DELIVERY_COMPLETED -> "تم تسجيل التوصيل بنجاح."
        NotificationType.DELIVERY_CANCELLED -> "تم إلغاء التوصيل."
    }

    companion object {
        private const val CHANNEL_DELIVERY_STATUS = "delivery_status"
        private const val CHANNEL_NAVIGATION_ALERTS = "navigation_alerts"

        private const val NOTIF_ID_APPROACHING = 1001
        private const val NOTIF_ID_ARRIVED = 1002
        private const val NOTIF_ID_COMPLETED = 1003
        private const val NOTIF_ID_CANCELLED = 1004

        private val VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)
    }
}
