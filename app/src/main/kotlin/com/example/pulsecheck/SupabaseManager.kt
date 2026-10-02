package com.example.pulsecheck

import android.content.Context
import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Supabase Manager for PulseCheck
 * Handles authentication, database operations, and real-time updates
 */
class SupabaseManager private constructor(private val context: Context) {
    
    private val tag = "SupabaseManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentAlertId: String? = null
    private var alertStartTime = 0L
    
    companion object {
        private const val MAX_ALERT_DURATION = 30 * 60 * 1000L // 30 minutes
        
        @Volatile
        private var instance: SupabaseManager? = null
        
        fun getInstance(context: Context): SupabaseManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Data Models
    // ═══════════════════════════════════════════════════════════════════════
    
    @Serializable
    data class UserProfile(
        val id: String? = null,
        @SerialName("user_id") val userId: String,
        @SerialName("full_name") val fullName: String,
        val phone: String,
        val email: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )
    
    @Serializable
    data class EmergencyContact(
        val id: String? = null,
        @SerialName("user_id") val userId: String,
        val name: String,
        val phone: String,
        val email: String? = null,
        val affiliation: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )
    
    @Serializable
    data class SOSAlert(
        val id: String? = null,
        @SerialName("user_id") val userId: String,
        @SerialName("trigger_type") val triggerType: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val location: String? = null,
        @SerialName("contacts_notified") val contactsNotified: Int = 0,
        val status: String = "active", // active, resolved, cancelled
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("resolved_at") val resolvedAt: String? = null
    )
    
    @Serializable
    data class LocationUpdate(
        val id: String? = null,
        @SerialName("alert_id") val alertId: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float? = null,
        val speed: Float? = null,
        @SerialName("created_at") val createdAt: String? = null
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // Authentication
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return try {
            if (!SupabaseConfig.isInitialized()) return false
            SupabaseConfig.client.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            Log.e(tag, "Error checking auth status", e)
            false
        }
    }
    
    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return try {
            SupabaseConfig.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sign up new user
     */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        callback: AuthCallback
    ) = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseConfig.isInitialized()) {
                callback.onFailure("Supabase not initialized")
                return@withContext
            }
            
            // Sign up with Supabase Auth
            val result = SupabaseConfig.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            val userId = result.id ?: run {
                callback.onFailure("Failed to get user ID")
                return@withContext
            }
            
            // Create user profile
            val profile = UserProfile(
                userId = userId,
                fullName = fullName,
                phone = phone,
                email = email
            )
            
            SupabaseConfig.client.from("user_profiles").insert(profile)
            
            callback.onSuccess(userId)
            Log.d(tag, "User signed up successfully: $userId")
            
        } catch (e: Exception) {
            Log.e(tag, "Sign up failed", e)
            callback.onFailure(e.message ?: "Sign up failed")
        }
    }
    
    /**
     * Sign in existing user
     */
    suspend fun signIn(
        email: String,
        password: String,
        callback: AuthCallback
    ) = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseConfig.isInitialized()) {
                callback.onFailure("Supabase not initialized")
                return@withContext
            }
            
            val result = SupabaseConfig.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            callback.onSuccess(result.id)
            Log.d(tag, "User signed in successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Sign in failed", e)
            callback.onFailure(e.message ?: "Sign in failed")
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(callback: AuthCallback? = null) = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseConfig.isInitialized()) {
                callback?.onFailure("Supabase not initialized")
                return@withContext
            }
            
            SupabaseConfig.client.auth.signOut()
            callback?.onSuccess(null)
            Log.d(tag, "User signed out successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Sign out failed", e)
            callback?.onFailure(e.message ?: "Sign out failed")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Emergency Contacts
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Sync local contacts to Supabase
     */
    suspend fun syncContacts(
        contacts: List<contact>,
        callback: SyncCallback
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated()) {
                callback.onFailure("User not authenticated")
                return@withContext
            }
            
            val userId = getUserId() ?: run {
                callback.onFailure("Failed to get user ID")
                return@withContext
            }
            
            // Delete existing contacts for this user
            SupabaseConfig.client.from("emergency_contacts")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }
            
            // Insert new contacts
            val supabaseContacts = contacts.map { contact ->
                EmergencyContact(
                    userId = userId,
                    name = contact.name,
                    phone = contact.phoneNumber,
                    email = contact.email.takeIf { it.isNotEmpty() },
                    affiliation = contact.affiliation.takeIf { it.isNotEmpty() }
                )
            }
            
            if (supabaseContacts.isNotEmpty()) {
                SupabaseConfig.client.from("emergency_contacts").insert(supabaseContacts)
            }
            
            callback.onSuccess(contacts.size)
            Log.d(tag, "Synced ${contacts.size} contacts to Supabase")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync contacts", e)
            callback.onFailure(e.message ?: "Sync failed")
        }
    }
    
    /**
     * Get contacts from Supabase
     */
    suspend fun getContacts(callback: ContactsCallback) = withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated()) {
                callback.onFailure("User not authenticated")
                return@withContext
            }
            
            val userId = getUserId() ?: run {
                callback.onFailure("Failed to get user ID")
                return@withContext
            }
            
            val result = SupabaseConfig.client.from("emergency_contacts")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<EmergencyContact>()
            
            callback.onSuccess(result)
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to get contacts", e)
            callback.onFailure(e.message ?: "Failed to fetch contacts")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SOS Alerts
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create SOS alert in Supabase
     */
    suspend fun createSOSAlert(
        triggerType: String,
        latitude: Double?,
        longitude: Double?,
        location: String?,
        contactsNotified: Int,
        callback: AlertCallback
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated()) {
                callback.onFailure("User not authenticated")
                return@withContext
            }
            
            val userId = getUserId() ?: run {
                callback.onFailure("Failed to get user ID")
                return@withContext
            }
            
            // Check for existing active alert
            if (currentAlertId != null && !isAlertExpired()) {
                callback.onSuccess(currentAlertId)
                return@withContext
            }
            
            val alert = SOSAlert(
                userId = userId,
                triggerType = triggerType,
                latitude = latitude,
                longitude = longitude,
                location = location,
                contactsNotified = contactsNotified,
                status = "active"
            )
            
            val result = SupabaseConfig.client.from("sos_alerts")
                .insert(alert)
                .decodeSingle<SOSAlert>()
            
            currentAlertId = result.id
            alertStartTime = System.currentTimeMillis()
            
            callback.onSuccess(result.id)
            Log.d(tag, "SOS alert created: ${result.id}")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to create SOS alert", e)
            callback.onFailure(e.message ?: "Failed to create alert")
        }
    }
    
    /**
     * Update location during active alert
     */
    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        speed: Float?,
        callback: LocationCallback? = null
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated() || currentAlertId == null || isAlertExpired()) {
                if (isAlertExpired() && currentAlertId != null) {
                    stopAlert(null)
                }
                callback?.onFailure("No active alert")
                return@withContext
            }
            
            val locationUpdate = LocationUpdate(
                alertId = currentAlertId!!,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                speed = speed
            )
            
            SupabaseConfig.client.from("location_updates").insert(locationUpdate)
            
            callback?.onSuccess()
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to update location", e)
            callback?.onFailure(e.message ?: "Location update failed")
        }
    }
    
    /**
     * Stop/resolve active alert
     */
    suspend fun stopAlert(callback: AlertCallback?) = withContext(Dispatchers.IO) {
        try {
            val alertId = currentAlertId ?: run {
                callback?.onFailure("No active alert")
                return@withContext
            }
            
            // Update alert status
            SupabaseConfig.client.from("sos_alerts")
                .update(
                    {
                        set("status", "resolved")
                        set("resolved_at", Date().toInstant().toString())
                    }
                ) {
                    filter {
                        eq("id", alertId)
                    }
                }
            
            currentAlertId = null
            alertStartTime = 0
            
            callback?.onSuccess(null)
            Log.d(tag, "Alert stopped: $alertId")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop alert", e)
            callback?.onFailure(e.message ?: "Failed to stop alert")
        }
    }
    
    /**
     * Get alert history
     */
    suspend fun getAlertHistory(callback: AlertHistoryCallback) = withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated()) {
                callback.onFailure("User not authenticated")
                return@withContext
            }
            
            val userId = getUserId() ?: run {
                callback.onFailure("Failed to get user ID")
                return@withContext
            }
            
            val result = SupabaseConfig.client.from("sos_alerts")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", ascending = false)
                }
                .decodeList<SOSAlert>()
            
            callback.onSuccess(result)
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to get alert history", e)
            callback.onFailure(e.message ?: "Failed to fetch history")
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Real-time Updates
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Subscribe to real-time location updates for an alert
     */
    fun subscribeToLocationUpdates(alertId: String, callback: RealtimeLocationCallback) {
        scope.launch {
            try {
                val channel = SupabaseConfig.client.realtime.channel("location-updates")
                
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "location_updates"
                    filter = "alert_id=eq.$alertId"
                }
                
                changeFlow.onEach { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            // New location update received
                            callback.onLocationUpdate(action.record.toString())
                        }
                        else -> {}
                    }
                }.launchIn(scope)
                
                channel.subscribe()
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to subscribe to location updates", e)
                callback.onError(e.message ?: "Subscription failed")
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════
    
    private fun isAlertExpired(): Boolean {
        return alertStartTime == 0L || 
               System.currentTimeMillis() - alertStartTime > MAX_ALERT_DURATION
    }
    
    fun getCurrentAlertId(): String? = currentAlertId
    
    fun isAlertActive(): Boolean = currentAlertId != null && !isAlertExpired()
    
    // ═══════════════════════════════════════════════════════════════════════
    // Callbacks
    // ═══════════════════════════════════════════════════════════════════════
    
    interface AuthCallback {
        fun onSuccess(userId: String?)
        fun onFailure(error: String)
    }
    
    interface SyncCallback {
        fun onSuccess(count: Int)
        fun onFailure(error: String)
    }
    
    interface ContactsCallback {
        fun onSuccess(contacts: List<EmergencyContact>)
        fun onFailure(error: String)
    }
    
    interface AlertCallback {
        fun onSuccess(alertId: String?)
        fun onFailure(error: String)
    }
    
    interface LocationCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }
    
    interface AlertHistoryCallback {
        fun onSuccess(alerts: List<SOSAlert>)
        fun onFailure(error: String)
    }
    
    interface RealtimeLocationCallback {
        fun onLocationUpdate(data: String)
        fun onError(error: String)
    }
}

    // ═══════════════════════════════════════════════════════════════════════
    // VIDEO RECORDING OPERATIONS (NEW - v2.0)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Upload video file to Supabase Storage
     * Path structure: {userId}/{timestamp}_{filename}
     */
    suspend fun uploadVideo(
        videoFile: java.io.File,
        alertId: String? = null,
        callback: VideoUploadCallback
    ) {
        if (!isAuthenticated()) {
            callback.onFailure("Not authenticated")
            return
        }
        
        scope.launch {
            try {
                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: run {
                    callback.onFailure("User ID not available")
                    return@launch
                }
                
                // Create file path in storage: userId/timestamp_filename
                val timestamp = System.currentTimeMillis()
                val storagePath = "$userId/${timestamp}_${videoFile.name}"
                
                android.util.Log.d("SupabaseManager", "Uploading video: ${videoFile.name} to $storagePath")
                
                // Upload to Supabase Storage
                val bytes = videoFile.readBytes()
                supabaseClient.storage.from("sos-videos").upload(storagePath, bytes)
                
                // Get public URL (or signed URL for private buckets)
                val videoUrl = supabaseClient.storage.from("sos-videos")
                    .createSignedUrl(storagePath, 31536000) // 1 year expiry
                
                android.util.Log.d("SupabaseManager", "Video uploaded successfully: $videoUrl")
                
                // Save video metadata to database
                val videoRecord = mapOf(
                    "user_id" to userId,
                    "alert_id" to alertId,
                    "file_name" to videoFile.name,
                    "file_path" to storagePath,
                    "storage_url" to videoUrl,
                    "duration_ms" to 0, // TODO: Calculate actual duration
                    "file_size_bytes" to videoFile.length(),
                    "recorded_at" to java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss", 
                        java.util.Locale.US
                    ).format(java.util.Date()),
                    "shared_count" to 0
                )
                
                supabaseClient.from("video_recordings").insert(videoRecord)
                
                callback.onSuccess(videoUrl, storagePath)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Video upload failed", e)
                callback.onFailure(e.message ?: "Upload failed")
            }
        }
    }
    
    /**
     * Log video sharing activity
     */
    suspend fun logVideoSharing(
        videoId: String,
        contactName: String,
        contactEmail: String?,
        contactPhone: String?,
        sharingMethod: String,
        callback: SyncCallback
    ) {
        if (!isAuthenticated()) {
            callback.onFailure("Not authenticated")
            return
        }
        
        scope.launch {
            try {
                val sharingLog = mapOf(
                    "video_id" to videoId,
                    "contact_name" to contactName,
                    "contact_email" to contactEmail,
                    "contact_phone" to contactPhone,
                    "sharing_method" to sharingMethod,
                    "delivery_status" to "sent"
                )
                
                supabaseClient.from("video_sharing_log").insert(sharingLog)
                
                callback.onSuccess(1)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Failed to log video sharing", e)
                callback.onFailure(e.message ?: "Failed to log sharing")
            }
        }
    }
    
    /**
     * Get all videos for current user
     */
    suspend fun getUserVideos(callback: VideosCallback) {
        if (!isAuthenticated()) {
            callback.onFailure("Not authenticated")
            return
        }
        
        scope.launch {
            try {
                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: run {
                    callback.onFailure("User ID not available")
                    return@launch
                }
                
                val response = supabaseClient.from("video_recordings")
                    .select() {
                        filter {
                            eq("user_id", userId)
                        }
                        order("recorded_at", ascending = false)
                    }
                
                // Parse response to video list
                val videos = mutableListOf<VideoRecordCloud>()
                // TODO: Parse JSON response to VideoRecordCloud objects
                
                callback.onSuccess(videos)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Failed to fetch videos", e)
                callback.onFailure(e.message ?: "Failed to fetch videos")
            }
        }
    }
    
    /**
     * Get video statistics for current user
     */
    suspend fun getVideoStatistics(callback: VideoStatsCallback) {
        if (!isAuthenticated()) {
            callback.onFailure("Not authenticated")
            return
        }
        
        scope.launch {
            try {
                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: run {
                    callback.onFailure("User ID not available")
                    return@launch
                }
                
                val response = supabaseClient.from("video_statistics")
                    .select() {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                
                // TODO: Parse response to VideoStats object
                val stats = VideoStats(
                    totalVideos = 0,
                    totalStorageBytes = 0,
                    totalDurationMs = 0,
                    avgDurationMs = 0,
                    totalShares = 0,
                    uploadedCount = 0
                )
                
                callback.onSuccess(stats)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Failed to fetch video statistics", e)
                callback.onFailure(e.message ?: "Failed to fetch statistics")
            }
        }
    }
    
    /**
     * Delete video from storage and database
     */
    suspend fun deleteVideo(videoId: String, storagePath: String, callback: SyncCallback) {
        if (!isAuthenticated()) {
            callback.onFailure("Not authenticated")
            return
        }
        
        scope.launch {
            try {
                // Delete from storage
                supabaseClient.storage.from("sos-videos").delete(storagePath)
                
                // Delete from database
                supabaseClient.from("video_recordings")
                    .delete {
                        filter {
                            eq("id", videoId)
                        }
                    }
                
                callback.onSuccess(1)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Failed to delete video", e)
                callback.onFailure(e.message ?: "Failed to delete video")
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NEW CALLBACK INTERFACES
    // ═══════════════════════════════════════════════════════════════════════
    
    interface VideoUploadCallback {
        fun onSuccess(videoUrl: String, storagePath: String)
        fun onFailure(error: String)
    }
    
    interface VideosCallback {
        fun onSuccess(videos: List<VideoRecordCloud>)
        fun onFailure(error: String)
    }
    
    interface VideoStatsCallback {
        fun onSuccess(stats: VideoStats)
        fun onFailure(error: String)
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA CLASSES FOR VIDEO
    // ═══════════════════════════════════════════════════════════════════════
    
    data class VideoRecordCloud(
        val id: String,
        val userId: String,
        val alertId: String?,
        val fileName: String,
        val storageUrl: String?,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val recordedAt: String,
        val sharedCount: Int
    )
    
    data class VideoStats(
        val totalVideos: Int,
        val totalStorageBytes: Long,
        val totalDurationMs: Long,
        val avgDurationMs: Long,
        val totalShares: Int,
        val uploadedCount: Int
    ) {
        fun getFormattedStorage(): String {
            val mb = totalStorageBytes / (1024.0 * 1024.0)
            return String.format("%.2f MB", mb)
        }
    }
}
