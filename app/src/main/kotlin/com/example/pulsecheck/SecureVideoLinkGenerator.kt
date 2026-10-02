package com.example.pulsecheck

import android.content.Context

class SecureVideoLinkGenerator(private val context: Context) {
    suspend fun generateSecureLink(videoId: String, expiryHours: Int = 72): Result<String> =
        Result.failure(UnsupportedOperationException("Cloud links are not configured"))

    suspend fun generateLinksForAlert(alertId: String, expiryHours: Int = 72): Result<String> =
        Result.failure(UnsupportedOperationException("Cloud links are not configured"))

    suspend fun generateShortLink(longUrl: String): Result<String> = Result.success(longUrl)
}
