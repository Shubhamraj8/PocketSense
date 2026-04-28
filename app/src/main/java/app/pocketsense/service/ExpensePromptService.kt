package app.pocketsense.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.pocketsense.MainActivity
import app.pocketsense.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExpensePromptService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var watcher: UsageWatcher

    override fun onCreate() {
        super.onCreate()
        watcher = UsageWatcher(this)
        ensureChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FG_NOTIFICATION_ID,
                buildOngoingNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(FG_NOTIFICATION_ID, buildOngoingNotification())
        }
        scope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        while (true) {
            try {
                watcher.pollNewExits().forEach { promptFor(it) }
            } catch (_: SecurityException) {
                // Usage access revoked — keep the service alive; it will resume
                // working when permission is restored.
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun promptFor(event: ExitEvent) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val tap = PendingIntent.getActivity(
            this,
            event.packageName.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_TRIGGER_PACKAGE, event.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val appLabel = PaymentApps.label(event.packageName)
        val notif = NotificationCompat.Builder(this, CHANNEL_PROMPTS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Did you spend in $appLabel?")
            .setContentText("Tap to log this expense.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        nm.notify(event.packageName.hashCode(), notif)
    }

    private fun buildOngoingNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("PocketSense is watching")
            .setContentText("Will prompt you after you use a payments app.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun ensureChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                "Background watcher",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Keeps PocketSense ready to prompt you." }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROMPTS,
                "Expense prompts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Asks if you spent money after using a payments app." }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ONGOING = "ps_ongoing"
        const val CHANNEL_PROMPTS = "ps_prompts"
        const val FG_NOTIFICATION_ID = 1
        const val EXTRA_TRIGGER_PACKAGE = "trigger_package"
        const val POLL_INTERVAL_MS = 6_000L

        fun start(context: Context) {
            val intent = Intent(context, ExpensePromptService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ExpensePromptService::class.java))
        }
    }
}
