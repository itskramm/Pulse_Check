package com.example.pulsecheck

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import java.util.Locale

object VerificationCodeManager {
    const val EXPIRY_MS = 10 * 60 * 1000L
    private const val PREFS = "PulseCheckVerifyPrefs"
    private const val K_EMAIL = "verify_email"
    private const val K_CODE = "verify_code"
    private const val K_TIME = "verify_issued_at"

    fun generateAndStore(ctx: Context, email: String?): String {
        val code = generateCode()
        prefs(ctx).edit()
            .putString(K_EMAIL, email?.trim()?.lowercase(Locale.ROOT) ?: "")
            .putString(K_CODE, code)
            .putLong(K_TIME, System.currentTimeMillis())
            .apply()
        return code
    }

    fun verify(ctx: Context, email: String?, enteredCode: String?): Boolean {
        val p = prefs(ctx)
        val storedEmail = p.getString(K_EMAIL, "") ?: ""
        val storedCode = p.getString(K_CODE, "") ?: ""
        val issuedAt = p.getLong(K_TIME, 0)
        if (email == null || enteredCode == null || storedCode.isEmpty()) return false
        val emailMatches = storedEmail == email.trim().lowercase(Locale.ROOT)
        val codeMatches = storedCode == enteredCode.trim()
        val notExpired = System.currentTimeMillis() - issuedAt <= EXPIRY_MS
        return if (emailMatches && codeMatches && notExpired) {
            clear(ctx)
            true
        } else false
    }

    fun hasActiveCode(ctx: Context): Boolean {
        val p = prefs(ctx)
        if ((p.getString(K_CODE, "") ?: "").isEmpty()) return false
        return System.currentTimeMillis() - p.getLong(K_TIME, 0) <= EXPIRY_MS
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(K_EMAIL).remove(K_CODE).remove(K_TIME).apply()
    }

    private fun generateCode() =
        String.format(Locale.US, "%06d", SecureRandom().nextInt(1_000_000))

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
