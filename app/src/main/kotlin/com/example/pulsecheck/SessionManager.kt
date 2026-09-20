package com.example.pulsecheck

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()

    fun setLoggedIn(loggedIn: Boolean) = editor.putBoolean(KEY_LOGGED_IN, loggedIn).apply()
    fun isLoggedIn() = prefs.getBoolean(KEY_LOGGED_IN, false)
    fun setDarkMode(enabled: Boolean) = editor.putBoolean(KEY_DARK_MODE, enabled).apply()
    fun isDarkMode() = prefs.getBoolean(KEY_DARK_MODE, false)
    fun saveUserName(name: String) = editor.putString(KEY_USER_NAME, name).apply()
    fun getUserName() = prefs.getString(KEY_USER_NAME, "User") ?: "User"
    fun saveUserPhone(phone: String) = editor.putString(KEY_USER_PHONE, phone).apply()
    fun getUserPhone() = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun saveUserEmail(email: String) = editor.putString(KEY_USER_EMAIL, email).apply()
    fun getUserEmail() = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun setCountdownSeconds(seconds: Int) =
        editor.putInt(KEY_COUNTDOWN_SECONDS, seconds.coerceIn(3, 10)).apply()

    fun getCountdownSeconds() = prefs.getInt(KEY_COUNTDOWN_SECONDS, 3)
    fun setOnboardingDone(done: Boolean) = editor.putBoolean(KEY_ONBOARDING_DONE, done).apply()
    fun isOnboardingDone() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun setTutorialSeen(seen: Boolean) = editor.putBoolean(KEY_TUTORIAL_SEEN, seen).apply()
    fun isTutorialSeen() = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)
    fun saveUserAvatar(drawableRes: Int) = editor.putInt(KEY_USER_AVATAR, drawableRes).apply()
    fun getUserAvatar() = prefs.getInt(KEY_USER_AVATAR, R.drawable.profile)
    fun setBiometricEnabled(enabled: Boolean) = editor.putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    fun isBiometricEnabled() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun clearSession() {
        editor.remove(KEY_LOGGED_IN).remove(KEY_USER_NAME).apply()
    }

    fun setSosActive(active: Boolean) {
        editor.putBoolean(KEY_SOS_ACTIVE, active)
        if (active) editor.putLong(KEY_SOS_START_TIME, System.currentTimeMillis())
        else editor.remove(KEY_SOS_START_TIME)
        editor.apply()
    }

    fun isSosActive() = prefs.getBoolean(KEY_SOS_ACTIVE, false)
    fun getSosStartTime() = prefs.getLong(KEY_SOS_START_TIME, 0)

    fun savePathData(distance: Double, duration: Long, points: Int) {
        editor.putFloat(KEY_PATH_DISTANCE, distance.toFloat())
            .putLong(KEY_PATH_DURATION, duration)
            .putInt(KEY_PATH_POINTS, points)
            .apply()
    }

    fun getPathDistance() = prefs.getFloat(KEY_PATH_DISTANCE, 0f).toDouble()
    fun getPathDuration() = prefs.getLong(KEY_PATH_DURATION, 0)
    fun getPathPoints() = prefs.getInt(KEY_PATH_POINTS, 0)
    fun clearPathData() {
        editor.remove(KEY_PATH_DISTANCE).remove(KEY_PATH_DURATION).remove(KEY_PATH_POINTS).apply()
    }

    companion object {
        private const val PREF_NAME = "PulseCheckPrefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_COUNTDOWN_SECONDS = "countdown_seconds"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        private const val KEY_USER_AVATAR = "user_avatar_res"
        private const val KEY_SOS_ACTIVE = "sos_active"
        private const val KEY_SOS_START_TIME = "sos_start_time"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PATH_DISTANCE = "path_distance"
        private const val KEY_PATH_DURATION = "path_duration"
        private const val KEY_PATH_POINTS = "path_points"
    }
}
