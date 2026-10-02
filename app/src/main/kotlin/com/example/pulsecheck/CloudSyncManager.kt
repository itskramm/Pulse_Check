package com.example.pulsecheck

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Cloud Sync Manager
 * Coordinates synchronization between local database and Supabase cloud
 * 
 * Features:
 * - Automatic sync of encrypted videos to cloud storage
 * - Background sync with retry logic
 * - Sync status tracking and notifications
 * - Bandwidth-aware uploads (WiFi preferred)
 * - Conflict resolution
 */
class CloudSyncManager(private val context: Context) {
    private val TAG = "CloudSyncManager"
    
    private val videoManager = SupabaseVideoManager(context)
    private val alertManager = SupabaseAlertManager(context)
    private val contactManager = SupabaseContactManager(context)
    private val secureStorage = SecureVideoStorage(context)
    private val videoDB = VideoDatabaseHelper(context)
    private val notificationDB = NotificationDatabaseHelper(context)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val SYNC_RETRY_DELAY_MS = 5000L
        private const val MAX_SYNC_RETRIES = 3
    }
    
    /**
     * Sync status for UI updates
     */
    data class SyncStatus(
        val isSyncing: Boolean = false,
        val totalVideos: Int = 0,
        val syncedVideos: Int = 0,
        val failedVideos: Int = 0,
        val lastSyncTime: Long = 0,
        val errorMessage: String? = null
    )
    
    private var currentSyncStatus = SyncStatus()
    private val syncListeners = mutableListOf<(SyncStatus) -> Unit>()
    
    /**
     * Register listener for sync status updates
     */
    fun addSyncListener(listener: (SyncStatus) -> Unit) {
        syncListeners.add(listener)
    }
    
    /**
     * Remove sync listener
     */
    fun removeSyncListener(listener: (SyncStatus) -> Unit) {
        syncListeners.remove(listener)
    }
    
    /**
     * Notify all listeners of sync status change
     */
    private fun notifySyncStatus(status: SyncStatus) {
        currentSyncStatus = status
        syncListeners.forEach { it(status) }
    }
    
    /**
     * Sync all unsynced videos to cloud
     * @param wifiOnly Only sync when connected to WiFi
     * @return Number of videos successfully synced
     */
    suspend fun syncVideosToCloud(wifiOnly: Boolean = true): Int = withContext(Dispatchers.IO) {
        try {
            // Check WiFi if required
            if (wifiOnly && !isWifiConnected()) {
                Log.d(TAG, "Sync skipped: WiFi not available")
                notifySyncStatus(currentSyncStatus.copy(
                    errorMessage = "WiFi connection required"
                ))
                return@withContext 0
            }
            
            // Get all unsynced videos
            val unsyncedVideos = videoDB.getUnsyncedVideos()
            
            if (unsyncedVideos.isEmpty()) {
                Log.d(TAG, "No videos to sync")
                return@withContext 0
            }
            
            Log.d(TAG, "Starting sync for ${unsyncedVideos.size} videos")
            notifySyncStatus(SyncStatus(
                isSyncing = true,
                totalVideos = unsyncedVideos.size,
                syncedVideos = 0,
                failedVideos = 0
            ))
            
            var successCount = 0
            var failureCount = 0
            
            // Sync each video
            for (videoRecord in unsyncedVideos) {
                val result = syncSingleVideo(videoRecord)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                }
                
                // Update progress
                notifySyncStatus(SyncStatus(
                    isSyncing = true,
                    totalVideos = unsyncedVideos.size,
                    syncedVideos = successCount,
                    failedVideos = failureCount
                ))
            }
            
            // Final status
            notifySyncStatus(SyncStatus(
                isSyncing = false,
                totalVideos = unsyncedVideos.size,
                syncedVideos = successCount,
                failedVideos = failureCount,
                lastSyncTime = System.currentTimeMillis()
            ))
            
            // Log notification
            notificationDB.addNotification(
                "Cloud Sync Complete",
                "Synced $successCount video(s) to cloud" + 
                    if (failureCount > 0) ", $failureCount failed" else "",
                NotificationDatabaseHelper.TYPE_CLOUD_SYNC
            )
            
            Log.d(TAG, "Sync complete: $successCount success, $failureCount failed")
            successCount
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            notifySyncStatus(currentSyncStatus.copy(
                isSyncing = false,
                errorMessage = e.message
            ))
            0
        }
    }
    
    /**
     * Sync a single video to cloud with retry logic
     */
    private suspend fun syncSingleVideo(
        videoRecord: VideoRecord,
        retryCount: Int = 0
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get the encrypted file
            val encryptedFile = File(videoRecord.filePath)
            if (!encryptedFile.exists()) {
                Log.e(TAG, "Encrypted file not found: ${videoRecord.filePath}")
                return@withContext Result.failure(Exception("File not found"))
            }
            
            // Calculate checksum for integrity verification
            val checksum = secureStorage.calculateChecksum(encryptedFile)
            
            // Prepare metadata
            val metadata = VideoMetadata(
                videoId = videoRecord.id,
                userId = SessionManager.getUserId(context) ?: "anonymous",
                alertId = videoRecord.alertId,
                durationSeconds = videoRecord.duration,
                fileSizeBytes = encryptedFile.length(),
                encryptionIV = videoRecord.encryptionIV ?: "",
                checksum = checksum
            )
            
            // Upload to Supabase
            val uploadResult = videoManager.uploadEncryptedVideo(encryptedFile, metadata)
            
            if (uploadResult.isSuccess) {
                // Mark as synced in local database
                videoDB.markVideoAsSynced(videoRecord.id)
                Log.d(TAG, "Video synced successfully: ${videoRecord.id}")
                return@withContext uploadResult
            } else {
                // Retry on failure
                if (retryCount < MAX_SYNC_RETRIES) {
                    Log.w(TAG, "Sync failed, retrying... (${retryCount + 1}/$MAX_SYNC_RETRIES)")
                    delay(SYNC_RETRY_DELAY_MS)
                    return@withContext syncSingleVideo(videoRecord, retryCount + 1)
                } else {
                    Log.e(TAG, "Sync failed after $MAX_SYNC_RETRIES retries: ${uploadResult.exceptionOrNull()?.message}")
                    return@withContext uploadResult
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing video: ${videoRecord.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create alert and sync initial data
     */
    suspend fun createAndSyncAlert(
        latitude: Double,
        longitude: Double,
        address: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = SessionManager.getUserId(context) ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            // Create alert in Supabase
            val alertResult = alertManager.createAlert(userId, latitude, longitude, address)
            
            if (alertResult.isFailure) {
                return@withContext alertResult
            }
            
            val alertId = alertResult.getOrThrow()
            
            // Sync contacts
            syncEmergencyContacts()
            
            Log.d(TAG, "Alert created and synced: $alertId")
            Result.success(alertId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create and sync alert", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add location update to existing alert
     */
    suspend fun addLocationUpdate(
        alertId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        address: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        alertManager.addLocationUpdate(alertId, latitude, longitude, accuracy, address)
    }
    
    /**
     * Resolve alert
     */
    suspend fun resolveAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        alertManager.resolveAlert(alertId)
    }
    
    /**
     * Sync emergency contacts to cloud
     */
    suspend fun syncEmergencyContacts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = SessionManager.getUserId(context) ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            val contactDB = ContactDatabaseHelper(context)
            val localContacts = contactDB.getAllContacts()
            
            // Convert to Supabase format
            val emergencyContacts = localContacts.map { contact ->
                EmergencyContact(
                    id = contact.id.toString(),
                    name = contact.name,
                    phone = contact.phone,
                    email = contact.email,
                    relationship = contact.relationship,
                    isPrimary = false, // You can add this field to your Contact class
                    notifyViaSms = true,
                    notifyViaEmail = contact.email?.isNotEmpty() == true
                )
            }
            
            contactManager.syncContacts(userId, emergencyContacts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync contacts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Log notification delivery
     */
    suspend fun logNotificationDelivery(
        alertId: String,
        contactId: String,
        notificationType: String,
        deliveryStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        alertManager.logNotification(alertId, contactId, notificationType, deliveryStatus)
    }
    
    /**
     * Get video statistics from cloud
     */
    suspend fun getVideoStatistics(): Result<VideoStatistics> = withContext(Dispatchers.IO) {
        try {
            val userId = SessionManager.getUserId(context) ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            videoManager.getVideoStatistics(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video statistics", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if WiFi is connected
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Get current sync status
     */
    fun getSyncStatus(): SyncStatus = currentSyncStatus
    
    /**
     * Cancel any ongoing sync operations
     */
    fun cancelSync() {
        scope.coroutineContext.cancelChildren()
        notifySyncStatus(currentSyncStatus.copy(
            isSyncing = false,
            errorMessage = "Sync cancelled by user"
        ))
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        syncListeners.clear()
    }
}

/**
 * Extension function for VideoDatabaseHelper to get unsynced videos
 */
fun VideoDatabaseHelper.getUnsyncedVideos(): List<VideoRecord> {
    val videos = mutableListOf<VideoRecord>()
    val db = readableDatabase
    
    val cursor = db.query(
        "videos",
        null,
        "is_synced = ?",
        arrayOf("0"),
        null,
        null,
        "recorded_at ASC"
    )
    
    cursor.use {
        while (it.moveToNext()) {
            val id = it.getString(it.getColumnIndexOrThrow("id"))
            val filePath = it.getString(it.getColumnIndexOrThrow("file_path"))
            val duration = it.getInt(it.getColumnIndexOrThrow("duration"))
            val fileSize = it.getLong(it.getColumnIndexOrThrow("file_size"))
            val isEncrypted = it.getInt(it.getColumnIndexOrThrow("is_encrypted")) == 1
            val encryptionIV = it.getString(it.getColumnIndexOrThrow("encryption_iv"))
            val recordedAt = it.getLong(it.getColumnIndexOrThrow("recorded_at"))
            val alertId = it.getString(it.getColumnIndexOrThrow("alert_id"))
            
            videos.add(VideoRecord(
                id = id,
                filePath = filePath,
                duration = duration,
                fileSize = fileSize,
                isEncrypted = isEncrypted,
                encryptionIV = encryptionIV,
                recordedAt = recordedAt,
                isSynced = false,
                shareCount = 0,
                alertId = alertId,
                viewableByUser = false
            ))
        }
    }
    
    return videos
}

/**
 * Extension function for VideoDatabaseHelper to mark video as synced
 */
fun VideoDatabaseHelper.markVideoAsSynced(videoId: String) {
    val db = writableDatabase
    val values = android.content.ContentValues().apply {
        put("is_synced", 1)
        put("synced_at", System.currentTimeMillis())
    }
    
    db.update("videos", values, "id = ?", arrayOf(videoId))
}

/**
 * Extension function for SecureVideoStorage to calculate checksum
 */
fun SecureVideoStorage.calculateChecksum(file: File): String {
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hash = digest.digest(bytes)
        hash.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        Log.e("SecureVideoStorage", "Failed to calculate checksum", e)
        ""
    }
}
