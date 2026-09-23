package com.onewheel.ridetracker.claude

import android.content.Context

/**
 * Stores the user's own Anthropic API key locally (SharedPreferences, on-device only —
 * never sent anywhere except as the auth header on requests to api.anthropic.com that
 * the user themselves triggers from the "Ask Claude" screen). Not encrypted at rest;
 * fine for a personal single-user device, but don't reuse this pattern for a shared phone.
 */
object ClaudeSettings {
    private const val PREFS = "claude_settings"
    private const val KEY_API_KEY = "api_key"

    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key.trim())
            .apply()
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_API_KEY)
            .apply()
    }
}
