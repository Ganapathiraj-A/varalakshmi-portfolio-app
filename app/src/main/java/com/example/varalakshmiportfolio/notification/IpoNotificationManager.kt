package com.example.varalakshmiportfolio.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.varalakshmiportfolio.MainActivity
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.IpoActionType

object IpoNotificationManager {
    const val CHANNEL_ID = "santhana_lakshmi_ipo_alerts"
    const val CHANNEL_NAME = "Santhana Lakshmi IPO Alerts"
    const val CHANNEL_DESC = "Actionable IPO application, mandate approval, and listing day exit alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postIpoAlert(context: Context, notification: IpoActionNotification) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Tap action: opens app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick Action button to open Kite IPO directly
        val kiteIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.deepLinkUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val kitePendingIntent = PendingIntent.getActivity(
            context,
            (notification.id + "_action").hashCode(),
            kiteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionButtonTitle = when (notification.actionType) {
            IpoActionType.APPLY_NOW -> "Apply on Kite"
            IpoActionType.MANDATE_PENDING -> "Approve on GPay"
            IpoActionType.LISTING_EXIT -> "View Order"
            else -> "Open App"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 " + notification.headline)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${notification.message}\n\n" +
                "• Bidding: ${notification.lotQuantity} Lot (${notification.lotShares} Shares)\n" +
                "• Bid Price: ${notification.formattedCutoffPrice} (Cut-off)\n" +
                "• Total Blocked: ${notification.formattedTotalAmount}\n" +
                "• QIB Demand: ${notification.formattedQibMultiple} | Closes: ${notification.closeDeadline}"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, actionButtonTitle, kitePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted or revoked
        }
    }

    fun cancelAlert(context: Context, notificationId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
        } catch (_: Exception) {
        }
    }

    fun cancelAllAlerts(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
        }
    }
}

