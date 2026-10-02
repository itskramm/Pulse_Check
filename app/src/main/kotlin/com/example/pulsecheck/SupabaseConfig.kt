package com.example.pulsecheck

import android.content.Context

object SupabaseConfig {
    const val SUPABASE_URL = "https://yfpmsaqxxfrqchkduhta.supabase.co"
    const val SUPABASE_ANON_KEY = "******"
    const val VIDEO_BUCKET_NAME = "sos-videos"
    const val MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024L
    const val DEBUG_MODE = false

    private const val PREFS = "supabase_settings"
    private const val ENABLED = "enabled"
    private const val URL = "url"
    private const val KEY = "key"

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotBlank() && getAnonKey(context).isNotBlank()

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()

    fun getUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(URL, SUPABASE_URL) ?: SUPABASE_URL

    fun getAnonKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SUPABASE_ANON_KEY) ?: SUPABASE_ANON_KEY

    fun saveCredentials(context: Context, url: String, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(URL, url).putString(KEY, key).apply()

    fun isInitialized() = false
}
