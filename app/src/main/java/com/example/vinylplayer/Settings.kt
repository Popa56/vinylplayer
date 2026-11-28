package com.example.vinylplayer

import android.content.Context

object Settings {
    private const val PREFS = "settings"

    internal lateinit var lastCtx: Context
        private set

    private fun p() = lastCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        lastCtx = ctx.applicationContext
        Sfx.enabled = soundsEnabled
    }

    var soundsEnabled: Boolean
        get() = p().getBoolean("sounds", true)
        set(v) {
            p().edit().putBoolean("sounds", v).apply()
            Sfx.enabled = v
        }

    var hapticEnabled: Boolean
        get() = p().getBoolean("haptic", true)
        set(v) { p().edit().putBoolean("haptic", v).apply() }

    var onlyVinylDefault: Boolean
        get() = p().getBoolean("onlyVinyl", false)
        set(v) { p().edit().putBoolean("onlyVinyl", v).apply() }

    var keepScreenOn: Boolean
        get() = p().getBoolean("keepScreenOn", true)
        set(v) { p().edit().putBoolean("keepScreenOn", v).apply() }

    var onboardingDone: Boolean
        get() = p().getBoolean("onboarding", false)
        set(v) { p().edit().putBoolean("onboarding", v).apply() }
}
