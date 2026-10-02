package com.example.pulsecheck

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Supabase Client Manager
 * Handles all interactions with Supabase backend including:
 * - Authentication
 * - Database operations (user profiles, contacts, alerts, videos)
 * - Storage operations (encrypted video uploads)
 * - Realtime subscriptions
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"
    
    // TODO: Replace with your actual Supabase credentials
    private const val SUPABASE_URL = "https://yfpmsaqxxfrqchkduhta.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcG1zYXF4eGZycWNoa2R1aHRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4OTcxMTYsImV4cCI6MjEwNjQ3MzExNn0.81aCYKib-biNwMi9WsfWbrPtkbw_3-cqaQ4aJqx-H20"
    
    private var initialized = false
    
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Auth)
        }
    }
    
    /**
     * Initialize Supabase client
     * Call this in Application.onCreate() or before first use
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        try {
            // Just access the client to trigger lazy initialization
            val _ = client
            initialized = true
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            throw e
        }
    }
    
    /**
     * Check if client is initialized
     */
    fun isInitialized(): Boolean = initialized
    
    /**
     * Get the Postgrest instance for database operations
     */
    val database get() = client.postgrest
    
    /**
     * Get the Storage instance for file operations
     */
    val storage get() = client.storage
    
    /**
     * Get the Realtime instance for subscriptions
     */
    val realtime get() = client.realtime
    
    /**
     * Get the Auth instance for authentication
     */
    val auth get() = client.auth
}

/**
 * Supabase Video Manager
 * Handles encrypted video uploads and metadata management
 */
class SupabaseVideoManager(private val context: Context) {
    private val TAG = "SupabaseVideoManager"
    
    companion object {
        private const val BUCKET_NAME = "sos-videos"
        private const val MAX_UPLOAD_SIZE = 100 * 1024 * 1024L // 100MB
    }
    
    /**
     * Upload encrypted video to Supabase Storage
     * @param encryptedFile The encrypted video file
     * @param metadata Video metadata
     * @return The storage path of the uploaded video
     */
    suspend fun uploadEncryptedVideo(
        encryptedFile: File,
        metadata: VideoMetadata
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate file size
            if (encryptedFile.length() > MAX_UPLOAD_SIZE) {
                return@withContext Result.failure(
                    Exception("Video file exceeds maximum size of ${MAX_UPLOAD_SIZE / 1024 / 1024}MB")
                )
            }
            
            // Generate unique storage path
            val userId = metadata.userId
            val videoId = metadata.videoId
            val storagePath = "$userId/$videoId.encrypted"
            
            // Upload to Supabase Storage
            val bytes = encryptedFile.readBytes()
            SupabaseClient.storage
                .from(BUCKET_NAME)
                .upload(storagePath, bytes)
            
            Log.d(TAG, "Video uploaded successfully: $storagePath")
            
            // Insert metadata into database
            val insertResult = insertVideoMetadata(metadata, storagePath)
            if (insertResult.isFailure) {
                // Cleanup storage if database insert fails
                try {
                    deleteVideoFromStorage(storagePath)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cleanup storage after database insert failure", e)
                }
                return@withContext insertResult
            }
            
            Result.success(storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video", e)
            Result.failure(e)
        }
    }
    
    /**
     * Insert video metadata into database
     */
    private suspend fun insertVideoMetadata(
        metadata: VideoMetadata,
        storagePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "id" to metadata.videoId,
                "user_id" to metadata.userId,
                "alert_id" to metadata.alertId,
                "storage_path" to storagePath,
                "duration_seconds" to metadata.durationSeconds,
                "file_size_bytes" to metadata.fileSizeBytes,
                "encrypted" to true,
                "encryption_algorithm" to metadata.encryptionAlgorithm,
                "encryption_iv" to metadata.encryptionIV,
                "recorded_at" to metadata.recordedAt,
                "synced_at" to System.currentTimeMillis(),
                "checksum" to metadata.checksum
            )
            
            SupabaseClient.database
                .from("video_recordings")
                .insert(data)
            
            Log.d(TAG, "Video metadata inserted: ${metadata.videoId}")
            Result.success(storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert video metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download encrypted video from Supabase Storage
     * @param storagePath The storage path of the video
     * @return The encrypted video bytes
     */
    suspend fun downloadEncryptedVideo(storagePath: String): Result<ByteArray> = 
        withContext(Dispatchers.IO) {
            try {
                val bytes = SupabaseClient.storage
                    .from(BUCKET_NAME)
                    .downloadAuthenticated(storagePath)
                
                Log.d(TAG, "Video downloaded successfully: $storagePath")
                Result.success(bytes)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download video", e)
                Result.failure(e)
            }
        }
    
    /**
     * Delete video from storage
     */
    private suspend fun deleteVideoFromStorage(storagePath: String) {
        SupabaseClient.storage
            .from(BUCKET_NAME)
            .delete(storagePath)
        Log.d(TAG, "Video deleted from storage: $storagePath")
    }
    
    /**
     * Mark video as shared in database
     */
    suspend fun markVideoAsShared(
        videoId: String,
        sharedWith: String,
        sharedVia: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "video_id" to videoId,
                "shared_with" to sharedWith,
                "shared_via" to sharedVia,
                "shared_at" to System.currentTimeMillis()
            )
            
            SupabaseClient.database
                .from("video_sharing_log")
                .insert(data)
            
            Log.d(TAG, "Video marked as shared: $videoId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark video as shared", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get video statistics for a user
     */
    suspend fun getVideoStatistics(userId: String): Result<VideoStatistics> = 
        withContext(Dispatchers.IO) {
            try {
                val response = SupabaseClient.database
                    .from("video_statistics")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<VideoStatisticsResponse>()
                
                val stats = VideoStatistics(
                    totalVideos = response.total_videos ?: 0,
                    totalSizeBytes = response.total_size_bytes ?: 0L,
                    totalShareCount = response.total_share_count ?: 0,
                    averageDurationSeconds = response.avg_duration_seconds ?: 0.0
                )
                
                Result.success(stats)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get video statistics", e)
                Result.failure(e)
            }
        }
}

/**
 * Data classes for Supabase operations
 */
data class VideoMetadata(
    val videoId: String = UUID.randomUUID().toString(),
    val userId: String,
    val alertId: String?,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val encryptionIV: String,
    val recordedAt: Long = System.currentTimeMillis(),
    val checksum: String
)

data class VideoStatistics(
    val totalVideos: Int,
    val totalSizeBytes: Long,
    val totalShareCount: Int,
    val averageDurationSeconds: Double
)

// Response models for Supabase queries
@kotlinx.serialization.Serializable
private data class VideoStatisticsResponse(
    val user_id: String? = null,
    val total_videos: Int? = null,
    val total_size_bytes: Long? = null,
    val total_share_count: Int? = null,
    val avg_duration_seconds: Double? = null
)

/**
 * Supabase Alert Manager
 * Handles SOS alerts and location updates
 */
class SupabaseAlertManager(private val context: Context) {
    private val TAG = "SupabaseAlertManager"
    
    /**
     * Create new SOS alert
     */
    suspend fun createAlert(
        userId: String,
        latitude: Double,
        longitude: Double,
        address: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val alertId = UUID.randomUUID().toString()
            val data = mapOf(
                "id" to alertId,
                "user_id" to userId,
                "latitude" to latitude,
                "longitude" to longitude,
                "address" to address,
                "status" to "active",
                "triggered_at" to System.currentTimeMillis()
            )
            
            SupabaseClient.database
                .from("sos_alerts")
                .insert(data)
            
            Log.d(TAG, "Alert created: $alertId")
            Result.success(alertId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create alert", e)
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
        address: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "alert_id" to alertId,
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy_meters" to accuracy,
                "address" to address,
                "recorded_at" to System.currentTimeMillis()
            )
            
            SupabaseClient.database
                .from("location_updates")
                .insert(data)
            
            Log.d(TAG, "Location update added for alert: $alertId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add location update", e)
            Result.failure(e)
        }
    }
    
    /**
     * Resolve alert (mark as resolved)
     */
    suspend fun resolveAlert(alertId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "status" to "resolved",
                    "resolved_at" to System.currentTimeMillis()
                )
                
                SupabaseClient.database
                    .from("sos_alerts")
                    .update(data) {
                        filter {
                            eq("id", alertId)
                        }
                    }
                
                Log.d(TAG, "Alert resolved: $alertId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve alert", e)
                Result.failure(e)
            }
        }
    
    /**
     * Log notification sent
     */
    suspend fun logNotification(
        alertId: String,
        contactId: String,
        notificationType: String,
        deliveryStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "alert_id" to alertId,
                "contact_id" to contactId,
                "notification_type" to notificationType,
                "delivery_status" to deliveryStatus,
                "sent_at" to System.currentTimeMillis()
            )
            
            SupabaseClient.database
                .from("alert_notifications")
                .insert(data)
            
            Log.d(TAG, "Notification logged for alert: $alertId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log notification", e)
            Result.failure(e)
        }
    }
}

/**
 * Supabase Contact Manager
 * Handles emergency contacts sync
 */
class SupabaseContactManager(private val context: Context) {
    private val TAG = "SupabaseContactManager"
    
    /**
     * Sync contacts to Supabase
     */
    suspend fun syncContacts(
        userId: String,
        contacts: List<EmergencyContact>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete existing contacts
            SupabaseClient.database
                .from("emergency_contacts")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }
            
            // Insert new contacts
            val contactData = contacts.map { contact ->
                mapOf(
                    "id" to contact.id,
                    "user_id" to userId,
                    "name" to contact.name,
                    "phone_number" to contact.phone,
                    "email" to contact.email,
                    "relationship" to contact.relationship,
                    "is_primary" to contact.isPrimary,
                    "notify_via_sms" to contact.notifyViaSms,
                    "notify_via_email" to contact.notifyViaEmail
                )
            }
            
            if (contactData.isNotEmpty()) {
                SupabaseClient.database
                    .from("emergency_contacts")
                    .insert(contactData)
            }
            
            Log.d(TAG, "Contacts synced: ${contacts.size} contacts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync contacts", e)
            Result.failure(e)
        }
    }
}

/**
 * Emergency contact data class for Supabase sync
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val relationship: String?,
    val isPrimary: Boolean = false,
    val notifyViaSms: Boolean = true,
    val notifyViaEmail: Boolean = false
)
