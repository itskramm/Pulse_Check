package com.example.pulsecheck

import android.content.Context
import java.io.File

class SupabaseManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: SupabaseManager? = null
        fun getInstance(context: Context): SupabaseManager =
            instance ?: synchronized(this) {
                instance ?: SupabaseManager(context.applicationContext).also { instance = it }
            }
    }

    data class UserProfile(val userId: String = "", val fullName: String = "", val phone: String = "", val email: String? = null)
    data class EmergencyContact(val name: String, val phone: String, val email: String? = null)
    data class SOSAlert(val id: String? = null, val triggerType: String = "", val status: String = "active")

    interface AuthCallback {
        fun onSuccess(userId: String?)
        fun onFailure(error: String)
    }
    interface SyncCallback {
        fun onSuccess(count: Int)
        fun onFailure(error: String)
    }
    interface AlertHistoryCallback {
        fun onSuccess(alerts: List<SOSAlert>)
        fun onFailure(error: String)
    }
    interface VideoUploadCallback {
        fun onSuccess(videoUrl: String, storagePath: String)
        fun onFailure(error: String)
    }

    fun isAuthenticated() = false
    fun getUserId(): String? = null
    suspend fun signOut(callback: AuthCallback? = null) {
        callback?.onSuccess(null)
    }
    fun uploadVideo(videoFile: File, alertId: String?, callback: VideoUploadCallback) {
        callback.onFailure("Cloud upload is not configured")
    }
    fun syncContacts(contacts: List<contact>, callback: SyncCallback) = callback.onSuccess(0)
    fun getAlertHistory(callback: AlertHistoryCallback) = callback.onSuccess(emptyList())
}
