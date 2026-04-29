package app.pocketsense

import android.app.Application
import app.pocketsense.data.AppContainer
import app.pocketsense.service.ExpensePromptService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PocketSenseApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (container.preferences.isWatcherEnabled()) {
            ExpensePromptService.start(this)
        }
        appScope.launch {
            // Auto-mark onboarded for upgrading users with existing data so they
            // don't get pushed back through the first-launch flow.
            if (!container.preferences.isOnboarded() && container.repository.hasAnyExpense()) {
                container.preferences.setOnboarded(true)
            }
            container.repository.ensureCurrentCycleAllocations()
        }
    }
}
