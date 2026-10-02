package com.example.pulsecheck

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoSharingHelper(private val context: Context) {
    interface SharingCallback {
        fun onSharingStarted(totalContacts: Int)
        fun onContactShared(contactName: String, method: String)
        fun onSharingComplete(successCount: Int, failedCount: Int)
        fun onSharingFailed(error: String)
    }

    suspend fun shareVideoWithContacts(
        videoFile: File,
        contacts: List<contact>,
        callback: SharingCallback
    ) = withContext(Dispatchers.Main) {
        if (contacts.isEmpty()) {
            callback.onSharingFailed("No contacts to share with")
            return@withContext
        }
        callback.onSharingStarted(contacts.size)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share video").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            contacts.forEach { callback.onContactShared(it.getName(), "Share") }
            callback.onSharingComplete(contacts.size, 0)
        } catch (error: Exception) {
            callback.onSharingFailed(error.message ?: "Unable to share video")
        }
    }
}
