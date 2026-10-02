package com.example.pulsecheck

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Helper class to share SOS videos with emergency contacts
 * 
 * Methods:
 * 1. MMS: Send video via multimedia message (requires cellular network)
 * 2. Email: Send video via email attachment
 * 3. Cloud Link: Upload to cloud and send link via SMS
 * 
 * Usage:
 * val sharing = VideoSharingHelper(context)
 * sharing.shareVideoWithContacts(videoFile, contacts) { success, failedCount ->
 *     if (success) {
 *         // All videos sent
 *     } else {
 *         // Some failed
 *     }
 * }
 */
class VideoSharingHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoSharingHelper"
        private const val MAX_MMS_SIZE_MB = 1 // Most carriers limit MMS to 1MB
        private const val MAX_MMS_SIZE_BYTES = MAX_MMS_SIZE_MB * 1024 * 1024
    }
    
    interface SharingCallback {
        fun onSharingStarted(totalContacts: Int)
        fun onContactShared(contactName: String, method: String)
        fun onSharingComplete(successCount: Int, failedCount: Int)
        fun onSharingFailed(error: String)
    }
    
    /**
     * Share video with all emergency contacts
     * Automatically chooses best method based on contact info
     */
    suspend fun shareVideoWithContacts(
        videoFile: File,
        contacts: List<contact>,
        callback: SharingCallback
    ) = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) {
            withContext(Dispatchers.Main) {
                callback.onSharingFailed("No contacts to share with")
            }
            return@withContext
        }
        
        withContext(Dispatchers.Main) {
            callback.onSharingStarted(contacts.size)
        }
        
        var successCount = 0
        var failedCount = 0
        
        val fileSize = videoFile.length()
        val videoUri = getFileUri(videoFile)
        
        Log.d(TAG, "Sharing video: ${videoFile.name}, size: ${fileSize / 1024} KB")
        
        for (contact in contacts) {
            try {
                val shared = when {
                    // If contact has email, send via email (more reliable for video)
                    contact.email.isNotEmpty() -> {
                        sendViaEmail(videoFile, videoUri, contact)
                    }
                    // If contact has phone and video is small enough, send via MMS
                    contact.phoneNumber.isNotEmpty() && fileSize <= MAX_MMS_SIZE_BYTES -> {
                        sendViaMMS(videoUri, contact)
                    }
                    // If contact has phone but video too large, send cloud link
                    contact.phoneNumber.isNotEmpty() -> {
                        sendCloudLink(videoFile, contact)
                    }
                    else -> {
                        Log.w(TAG, "Contact ${contact.name} has no valid phone or email")
                        false
                    }
                }
                
                if (shared) {
                    successCount++
                    withContext(Dispatchers.Main) {
                        val method = when {
                            contact.email.isNotEmpty() -> "Email"
                            fileSize <= MAX_MMS_SIZE_BYTES -> "MMS"
                            else -> "Link"
                        }
                        callback.onContactShared(contact.name, method)
                    }
                } else {
                    failedCount++
                }
                
                // Small delay between sends to avoid rate limiting
                Thread.sleep(500)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share with ${contact.name}", e)
                failedCount++
            }
        }
        
        withContext(Dispatchers.Main) {
            callback.onSharingComplete(successCount, failedCount)
        }
    }
    
    /**
     * Send video via MMS
     * Note: MMS size limits vary by carrier (usually 1-3 MB)
     */
    private fun sendViaMMS(videoUri: Uri, contact: contact): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra("address", contact.phoneNumber)
                putExtra("sms_body", "🚨 EMERGENCY VIDEO from PulseCheck\nTime: ${getCurrentTimestamp()}")
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(intent, "Send MMS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            
            Log.d(TAG, "MMS sent to ${contact.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send MMS to ${contact.name}", e)
            false
        }
    }
    
    /**
     * Send video via email attachment
     * Most reliable method for larger files
     */
    private fun sendViaEmail(videoFile: File, videoUri: Uri, contact: contact): Boolean {
        return try {
            val location = getLastKnownLocation()
            val timestamp = getCurrentTimestamp()
            
            val emailBody = buildString {
                appendLine("🚨 EMERGENCY ALERT from PulseCheck")
                appendLine()
                appendLine("This is an automated SOS alert with video evidence.")
                appendLine()
                appendLine("Time: $timestamp")
                appendLine("Location: $location")
                appendLine()
                appendLine("Video file attached: ${videoFile.name}")
                appendLine("Video size: ${videoFile.length() / 1024} KB")
                appendLine()
                appendLine("Please respond immediately or contact emergency services (911).")
                appendLine()
                appendLine("---")
                appendLine("Sent automatically by PulseCheck Safety App")
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // Email only
                putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
                putExtra(Intent.EXTRA_SUBJECT, "🚨 EMERGENCY ALERT - Video Evidence")
                putExtra(Intent.EXTRA_TEXT, emailBody)
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(intent, "Send Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            
            Log.d(TAG, "Email sent to ${contact.name} at ${contact.email}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email to ${contact.name}", e)
            false
        }
    }
    
    /**
     * Upload video to cloud and send link via SMS
     * Best for large videos or when MMS not available
     */
    private suspend fun sendCloudLink(videoFile: File, contact: contact): Boolean {
        return try {
            // Check if Supabase is available
            if (!SupabaseConfig.isEnabled(context)) {
                Log.w(TAG, "Cloud upload not available, Supabase not configured")
                return sendViaIntent(videoFile, contact)
            }
            
            // Upload to Supabase Storage
            val cloudUrl = uploadToSupabase(videoFile)
            
            if (cloudUrl != null) {
                // Send SMS with link
                val message = buildString {
                    appendLine("🚨 EMERGENCY ALERT from PulseCheck")
                    appendLine()
                    appendLine("Video evidence: $cloudUrl")
                    appendLine()
                    appendLine("Time: ${getCurrentTimestamp()}")
                    appendLine("Location: ${getLastKnownLocation()}")
                }
                
                sendSMS(contact.phoneNumber, message)
                Log.d(TAG, "Cloud link sent to ${contact.name}")
                true
            } else {
                // Fallback to intent
                sendViaIntent(videoFile, contact)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send cloud link to ${contact.name}", e)
            false
        }
    }
    
    /**
     * Fallback: Use system share intent
     */
    private fun sendViaIntent(videoFile: File, contact: contact): Boolean {
        return try {
            val videoUri = getFileUri(videoFile)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                putExtra(Intent.EXTRA_TEXT, "🚨 EMERGENCY VIDEO from PulseCheck")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via intent", e)
            false
        }
    }
    
    /**
     * Upload video to Supabase Storage
     */
    private suspend fun uploadToSupabase(videoFile: File): String? {
        return try {
            val bucket = "sos-videos"
            val fileName = "public/${videoFile.name}"
            
            // Upload file
            SupabaseConfig.client.storage
                .from(bucket)
                .upload(fileName, videoFile.readBytes())
            
            // Get public URL
            val url = SupabaseConfig.client.storage
                .from(bucket)
                .publicUrl(fileName)
            
            Log.d(TAG, "Video uploaded to cloud: $url")
            url
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to Supabase", e)
            null
        }
    }
    
    /**
     * Send SMS message
     */
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // Split long messages into multiple parts
            val parts = smsManager.divideMessage(message)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            
            Log.d(TAG, "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
    
    /**
     * Get file URI using FileProvider
     */
    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Get current timestamp
     */
    private fun getCurrentTimestamp(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    
    /**
     * Get last known location (if available)
     */
    private fun getLastKnownLocation(): String {
        // This should be passed from the calling activity
        // For now, return placeholder
        return "Location available in main alert"
    }
    
    /**
     * Check if video size is suitable for MMS
     */
    fun isMMSCompatible(videoFile: File): Boolean {
        return videoFile.length() <= MAX_MMS_SIZE_BYTES
    }
    
    /**
     * Get recommended sharing method for a contact
     */
    fun getRecommendedMethod(videoFile: File, contact: contact): String {
        return when {
            contact.email.isNotEmpty() -> "Email (Most Reliable)"
            contact.phoneNumber.isNotEmpty() && isMMSCompatible(videoFile) -> "MMS"
            contact.phoneNumber.isNotEmpty() -> "Cloud Link via SMS"
            else -> "Manual Sharing Required"
        }
    }
    
    /**
     * Estimate data usage for sharing
     */
    fun estimateDataUsage(videoFile: File, contactCount: Int): String {
        val sizePerContact = videoFile.length()
        val totalSize = sizePerContact * contactCount
        val totalMB = totalSize / (1024.0 * 1024.0)
        
        return "Approx. %.1f MB will be sent".format(totalMB)
    }
}
