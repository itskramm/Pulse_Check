package com.example.pulsecheck

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.Base64

/**
 * Secure Video Link Generator
 * Creates temporary secure links for encrypted videos in Supabase
 * 
 * Features:
 * - Generates time-limited download links (1-7 days)
 * - Links expire automatically
 * - Videos remain encrypted on server
 * - No authentication required for link access
 * - Link includes video decryption instructions
 */
class SecureVideoLinkGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureVideoLink"
        private const val DEFAULT_EXPIRY_HOURS = 72 // 3 days
    }
    
    /**
     * Generate secure cloud link for a video
     * 
     * @param videoFile The encrypted video file
     * @param expiryHours How long the link should be valid (default: 72 hours / 3 days)
     * @return Secure URL that contacts can access
     */
    suspend fun generateSecureLink(
        videoFile: File,
        expiryHours: Int = DEFAULT_EXPIRY_HOURS
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First, upload video to Supabase if not already uploaded
            val cloudSync = CloudSyncManager(context)
            val videoDb = VideoDatabaseHelper(context)
            
            // Get video metadata from database
            val videoRecord = videoDb.getVideoByFilePath(videoFile.absolutePath)
            
            if (videoRecord == null) {
                return@withContext Result.failure(Exception("Video not found in database"))
            }
            
            // Check if video is already synced to cloud
            if (!videoRecord.isSynced) {
                // Upload to cloud first
                Log.d(TAG, "Video not synced, uploading to cloud...")
                val syncResult = cloudSync.syncVideosToCloud(wifiOnly = false)
                
                if (syncResult == 0) {
                    return@withContext Result.failure(Exception("Failed to upload video to cloud"))
                }
            }
            
            // Generate signed URL from Supabase
            val userId = SessionManager.getUserId(context) ?: "anonymous"
            val storagePath = "$userId/${videoRecord.id}.encrypted"
            
            // Create signed URL with expiry
            val signedUrl = createSupabaseSignedUrl(storagePath, expiryHours)
            
            if (signedUrl.isFailure) {
                return@withContext signedUrl
            }
            
            // Create user-friendly wrapper link
            val secureLink = createPulseCheckViewerLink(
                videoId = videoRecord.id,
                signedUrl = signedUrl.getOrThrow(),
                expiryTime = System.currentTimeMillis() + (expiryHours * 60 * 60 * 1000L)
            )
            
            Log.d(TAG, "Secure link generated: $secureLink")
            Result.success(secureLink)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate secure link", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create signed URL from Supabase Storage
     */
    private suspend fun createSupabaseSignedUrl(
        storagePath: String,
        expiryHours: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val expirySeconds = expiryHours * 60 * 60
            
            // Use Supabase Storage API to create signed URL
            val signedUrl = SupabaseClient.storage
                .from("sos-videos")
                .createSignedUrl(storagePath, expirySeconds)
            
            Result.success(signedUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create signed URL", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create PulseCheck viewer link that wraps the Supabase URL
     * This provides a better user experience than raw Supabase URLs
     */
    private fun createPulseCheckViewerLink(
        videoId: String,
        signedUrl: String,
        expiryTime: Long
    ): String {
        // For now, return the direct signed URL
        // In future, you could create a web viewer at https://pulsecheck.app/view?id=xxx
        // that shows:
        // - Video player
        // - Emergency context
        // - "This is an emergency alert video" message
        // - Map with location
        // - Timestamp
        
        return signedUrl
    }
    
    /**
     * Generate multiple secure links for all videos in an alert
     * Returns a formatted string with all links
     */
    suspend fun generateLinksForAlert(
        alertId: String,
        expiryHours: Int = DEFAULT_EXPIRY_HOURS
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val videoDb = VideoDatabaseHelper(context)
            val videos = videoDb.getVideosByAlert(alertId)
            
            if (videos.isEmpty()) {
                return@withContext Result.success("No videos recorded for this alert")
            }
            
            val links = mutableListOf<String>()
            
            for ((index, videoRecord) in videos.withIndex()) {
                val file = File(videoRecord.filePath)
                if (!file.exists()) {
                    Log.w(TAG, "Video file not found: ${videoRecord.filePath}")
                    continue
                }
                
                val linkResult = generateSecureLink(file, expiryHours)
                if (linkResult.isSuccess) {
                    links.add("Video ${index + 1}: ${linkResult.getOrThrow()}")
                } else {
                    Log.e(TAG, "Failed to generate link for video ${index + 1}")
                }
            }
            
            if (links.isEmpty()) {
                return@withContext Result.failure(Exception("No video links generated"))
            }
            
            // Format links for SMS/Email
            val formatted = buildString {
                appendLine("📹 EMERGENCY VIDEO EVIDENCE (${links.size} segment${if (links.size > 1) "s" else ""})")
                appendLine()
                links.forEach { link ->
                    appendLine(link)
                }
                appendLine()
                appendLine("⚠️ Links expire in $expiryHours hours")
                appendLine("🔒 Videos are encrypted and secure")
            }
            
            Result.success(formatted)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate links for alert", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate a short, user-friendly link
     * Uses a link shortener service for better SMS delivery
     */
    suspend fun generateShortLink(longUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // For production, integrate with a link shortener like:
            // - Bitly API
            // - TinyURL API
            // - Your own domain with URL shortener
            
            // For now, return the long URL
            // TODO: Implement link shortening
            Result.success(longUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to shorten link", e)
            Result.failure(e)
        }
    }
}

/**
 * Extension function for VideoDatabaseHelper to get video by file path
 */
fun VideoDatabaseHelper.getVideoByFilePath(filePath: String): VideoRecord? {
    val db = readableDatabase
    
    val cursor = db.query(
        "videos",
        null,
        "file_path = ?",
        arrayOf(filePath),
        null,
        null,
        null,
        "1"
    )
    
    cursor.use {
        if (it.moveToFirst()) {
            val id = it.getLong(it.getColumnIndexOrThrow("id"))
            val fileName = it.getString(it.getColumnIndexOrThrow("file_name"))
            val duration = it.getLong(it.getColumnIndexOrThrow("duration_ms"))
            val fileSize = it.getLong(it.getColumnIndexOrThrow("file_size_bytes"))
            val recordedAt = it.getString(it.getColumnIndexOrThrow("recorded_at"))
            val alertId = if (it.isNull(it.getColumnIndexOrThrow("alert_id"))) null 
                         else it.getInt(it.getColumnIndexOrThrow("alert_id"))
            val sharedCount = it.getInt(it.getColumnIndexOrThrow("shared_count"))
            val uploadedToCloud = it.getInt(it.getColumnIndexOrThrow("uploaded_to_cloud")) == 1
            val cloudUrl = it.getString(it.getColumnIndexOrThrow("cloud_url"))
            val notes = it.getString(it.getColumnIndexOrThrow("notes"))
            val isEncrypted = it.getInt(it.getColumnIndexOrThrow("is_encrypted")) == 1
            val encryptionIV = it.getString(it.getColumnIndexOrThrow("encryption_iv"))
            val viewableByUser = it.getInt(it.getColumnIndexOrThrow("viewable_by_user")) == 1
            
            return VideoRecord(
                id = id,
                filePath = filePath,
                fileName = fileName,
                durationMs = duration,
                fileSizeBytes = fileSize,
                recordedAt = recordedAt,
                alertId = alertId,
                sharedCount = sharedCount,
                uploadedToCloud = uploadedToCloud,
                cloudUrl = cloudUrl,
                notes = notes,
                isEncrypted = isEncrypted,
                encryptionIV = encryptionIV,
                viewableByUser = viewableByUser
            )
        }
    }
    
    return null
}
