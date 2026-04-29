package app.pocketsense.data

import android.content.Context
import android.content.SharedPreferences
import app.pocketsense.service.PaymentApps

enum class DarkModePref { SYSTEM, LIGHT, DARK }

class Preferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(v: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDED, v).apply()
    }

    fun darkMode(): DarkModePref =
        runCatching { DarkModePref.valueOf(prefs.getString(KEY_DARK_MODE, null) ?: "SYSTEM") }
            .getOrDefault(DarkModePref.SYSTEM)

    fun setDarkMode(pref: DarkModePref) {
        prefs.edit().putString(KEY_DARK_MODE, pref.name).apply()
    }

    fun watchedPaymentApps(): Set<String> {
        val saved = prefs.getStringSet(KEY_WATCHED_PAYMENT_APPS, null)
        return if (saved.isNullOrEmpty()) PaymentApps.defaultPackages else saved.toSet()
    }

    fun setWatchedPaymentApps(packages: Set<String>) {
        val toSave = if (packages.isEmpty()) PaymentApps.defaultPackages else packages
        prefs.edit().putStringSet(KEY_WATCHED_PAYMENT_APPS, toSave).apply()
    }

    private companion object {
        const val NAME = "pocketsense_prefs"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_WATCHED_PAYMENT_APPS = "watched_payment_apps"
    }
}
