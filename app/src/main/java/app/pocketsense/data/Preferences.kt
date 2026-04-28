package app.pocketsense.data

import android.content.Context
import android.content.SharedPreferences

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

    private companion object {
        const val NAME = "pocketsense_prefs"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
