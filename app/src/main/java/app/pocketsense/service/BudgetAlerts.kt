package app.pocketsense.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.pocketsense.MainActivity
import app.pocketsense.R
import app.pocketsense.data.Category
import app.pocketsense.data.formatRupees

interface BudgetAlertSink {
    fun emit(category: Category, allocatedPaise: Long, spentPaise: Long, threshold: Float)
}

class AndroidBudgetAlertSink(private val context: Context) : BudgetAlertSink {

    override fun emit(category: Category, allocatedPaise: Long, spentPaise: Long, threshold: Float) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(nm)

        val (title, isOver) = if (threshold >= 1f) {
            "${category.name} budget exceeded" to true
        } else {
            "${category.name} at ${(threshold * 100).toInt()}%" to false
        }
        val text = "${formatRupees(spentPaise)} of ${formatRupees(allocatedPaise)} this cycle"

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_CATEGORY_ID, category.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val tap = PendingIntent.getActivity(
            context,
            (category.id * 10 + if (isOver) 1 else 0).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

        nm.notify(notificationIdFor(category.id, isOver), notif)
    }

    private fun ensureChannel(nm: NotificationManager) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Budget alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Tells you when a category crosses 80% or 100% of its cycle budget."
            }
        )
    }

    private fun notificationIdFor(categoryId: Long, over: Boolean): Int {
        // Stable per-category ID so 80% and 100% don't stack indefinitely.
        return (BASE_ID + categoryId.toInt() * 2 + if (over) 1 else 0)
    }

    companion object {
        const val CHANNEL_ID = "ps_budget_alerts"
        const val EXTRA_OPEN_CATEGORY_ID = "open_category_id"
        private const val BASE_ID = 1000
    }
}
