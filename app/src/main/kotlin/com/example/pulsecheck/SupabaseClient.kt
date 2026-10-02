package com.example.pulsecheck

import android.content.Context
import java.io.File

object SupabaseClient {
    private var initialized = false
    fun initialize(context: Context) {
        initialized = true
    }
    fun isInitialized() = initialized
}

class SupabaseVideoManager(private val context: Context) {
    suspend fun uploadEncryptedVideo(encryptedFile: File, metadata: VideoMetadata): Result<String> =
        Result.failure(UnsupportedOperationException("Cloud upload is not configured"))
}

data class VideoMetadata(
    val videoId: String,
    val userId: String,
    val alertId: String?,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val encryptionIV: String,
    val recordedAt: String = "",
    val checksum: String = ""
)
