package app.pocketsense.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

data class ExitEvent(
    val packageName: String,
    val exitedAt: Long,
    val sessionMs: Long,
)

class UsageWatcher(context: Context) {

    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var cursorTimestamp: Long = System.currentTimeMillis()
    private val foregroundEnter = mutableMapOf<String, Long>()
    private val lastPromptedAt = mutableMapOf<String, Long>()

    fun pollNewExits(): List<ExitEvent> {
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(cursorTimestamp, now)
        val out = mutableListOf<ExitEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in PaymentApps.packages) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundEnter[pkg] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val enteredAt = foregroundEnter.remove(pkg) ?: continue
                    val sessionMs = event.timeStamp - enteredAt
                    if (sessionMs < MIN_SESSION_MS) continue
                    val lastPrompt = lastPromptedAt[pkg] ?: 0L
                    if (event.timeStamp - lastPrompt < COOLDOWN_MS) continue
                    lastPromptedAt[pkg] = event.timeStamp
                    out += ExitEvent(pkg, event.timeStamp, sessionMs)
                }
            }
        }
        cursorTimestamp = now
        return out
    }

    companion object {
        // Filter incidental opens — must spend at least 8s in the app to count.
        const val MIN_SESSION_MS = 8_000L
        // Don't re-prompt for the same app within 2 minutes.
        const val COOLDOWN_MS = 120_000L
    }
}
