package com.example.pulsecheck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity to view and manage recorded SOS videos
 * Now integrated with VideoDatabaseHelper for complete metadata
 */
class VideoGalleryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvTotalVideos: TextView
    private lateinit var tvTotalStorage: TextView
    private lateinit var tvTotalShares: TextView
    private lateinit var tvCloudStatus: TextView
    private lateinit var btnSyncCloud: Button
    
    private lateinit var videoDb: VideoDatabaseHelper
    private val videos = mutableListOf<VideoRecord>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)
        
        videoDb = VideoDatabaseHelper(this)
        
        // Initialize views
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener { finish() }
        
        recyclerView = findViewById(R.id.recycler_videos)
        emptyState = findViewById(R.id.layout_empty_state)
        tvTotalVideos = findViewById(R.id.tv_total_videos)
        tvTotalStorage = findViewById(R.id.tv_total_storage)
        tvTotalShares = findViewById(R.id.tv_total_shares)
        tvCloudStatus = findViewById(R.id.tv_cloud_status)
        btnSyncCloud = findViewById(R.id.btn_sync_cloud)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        btnSyncCloud.setOnClickListener {
            syncUnuploadedVideos()
        }
        
        loadVideos()
        updateStatistics()
    }
    
    override fun onResume() {
        super.onResume()
        loadVideos()
        updateStatistics()
    }
    
    /**
     * Load videos from database
     */
    private fun loadVideos() {
        videos.clear()
        videos.addAll(videoDb.getAllVideos())
        
        if (videos.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            recyclerView.adapter = VideoAdapter(videos)
        }
    }
    
    /**
     * Update statistics display
     */
    private fun updateStatistics() {
        val totalVideos = videoDb.getVideoCount()
        val totalStorage = videoDb.getTotalStorageUsed()
        val unuploaded = videoDb.getUnuploadedVideos().size
        
        // Total videos
        tvTotalVideos.text = "$totalVideos video${if (totalVideos != 1) "s" else ""}"
        
        // Total storage
        val storageMB = totalStorage / (1024.0 * 1024.0)
        tvTotalStorage.text = String.format("%.2f MB used", storageMB)
        
        // Total shares
        var totalShares = 0
        videos.forEach { totalShares += it.sharedCount }
        tvTotalShares.text = "$totalShares share${if (totalShares != 1) "s" else ""}"
        
        // Cloud sync status
        if (unuploaded > 0) {
            tvCloudStatus.text = "$unuploaded video${if (unuploaded != 1) "s" else ""} pending upload"
            tvCloudStatus.setTextColor(getColor(R.color.pulse_alert))
            btnSyncCloud.visibility = View.VISIBLE
        } else {
            tvCloudStatus.text = "All synced ✓"
            tvCloudStatus.setTextColor(getColor(R.color.pulse_primary))
            btnSyncCloud.visibility = View.GONE
        }
    }
    
    /**
     * Sync unuploaded videos to cloud
     */
    private fun syncUnuploadedVideos() {
        val unuploaded = videoDb.getUnuploadedVideos()
        
        if (unuploaded.isEmpty()) {
            Toast.makeText(this, "All videos already synced", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Uploading ${unuploaded.size} videos...", Toast.LENGTH_SHORT).show()
        btnSyncCloud.isEnabled = false
        btnSyncCloud.text = "Uploading..."
        
        var uploadedCount = 0
        var failedCount = 0
        
        unuploaded.forEach { videoRecord ->
            val videoFile = File(videoRecord.filePath)
            
            if (!videoFile.exists()) {
                failedCount++
                android.util.Log.e("VideoGallery", "File not found: ${videoRecord.filePath}")
                return@forEach
            }
            
            val supabaseManager = SupabaseManager.getInstance()
            
            if (!supabaseManager.isAuthenticated()) {
                Toast.makeText(this, 
                    "Not authenticated. Please setup cloud sync first.", 
                    Toast.LENGTH_LONG).show()
                btnSyncCloud.isEnabled = true
                btnSyncCloud.text = "Sync to Cloud"
                return
            }
            
            // Upload to cloud
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    supabaseManager.uploadVideo(
                        videoFile = videoFile,
                        alertId = null,
                        callback = object : SupabaseManager.VideoUploadCallback {
                            override fun onSuccess(videoUrl: String, storagePath: String) {
                                uploadedCount++
                                videoDb.updateVideoCloudStatus(videoRecord.id, videoUrl)
                                
                                if (uploadedCount + failedCount == unuploaded.size) {
                                    onSyncComplete(uploadedCount, failedCount)
                                }
                            }
                            
                            override fun onFailure(error: String) {
                                failedCount++
                                android.util.Log.e("VideoGallery", "Upload failed: $error")
                                
                                if (uploadedCount + failedCount == unuploaded.size) {
                                    onSyncComplete(uploadedCount, failedCount)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    failedCount++
                    android.util.Log.e("VideoGallery", "Upload error", e)
                    
                    if (uploadedCount + failedCount == unuploaded.size) {
                        onSyncComplete(uploadedCount, failedCount)
                    }
                }
            }
        }
    }
    
    /**
     * Called when sync completes
     */
    private fun onSyncComplete(uploadedCount: Int, failedCount: Int) {
        runOnUiThread {
            btnSyncCloud.isEnabled = true
            btnSyncCloud.text = "Sync to Cloud"
            
            if (failedCount == 0) {
                Toast.makeText(this, 
                    "✓ $uploadedCount videos synced successfully", 
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, 
                    "⚠ $uploadedCount synced, $failedCount failed", 
                    Toast.LENGTH_LONG).show()
            }
            
            loadVideos()
            updateStatistics()
        }
    }
    
    /**
     * RecyclerView Adapter for video list
     */
    inner class VideoAdapter(private val videoList: List<VideoRecord>) : 
        RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video, parent, false)
            return VideoViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            holder.bind(videoList[position])
        }
        
        override fun getItemCount() = videoList.size
        
        inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
            private val tvFileDate: TextView = itemView.findViewById(R.id.tv_file_date)
            private val tvFileDuration: TextView = itemView.findViewById(R.id.tv_file_duration)
            private val tvFileSize: TextView = itemView.findViewById(R.id.tv_file_size)
            private val tvSharedCount: TextView = itemView.findViewById(R.id.tv_shared_count)
            private val ivCloudIcon: ImageView = itemView.findViewById(R.id.iv_cloud_icon)
            private val btnPlay: TextView = itemView.findViewById(R.id.btn_play)
            private val btnDelete: TextView = itemView.findViewById(R.id.btn_delete)
            private val btnShare: TextView = itemView.findViewById(R.id.btn_share)
            private val btnInfo: TextView = itemView.findViewById(R.id.btn_info)
            
            fun bind(videoRecord: VideoRecord) {
                // File name
                tvFileName.text = videoRecord.fileName
                
                // Date
                tvFileDate.text = "Recorded: ${videoRecord.recordedAt}"
                
                // Duration
                tvFileDuration.text = "⏱ ${videoRecord.getFormattedDuration()}"
                
                // Size
                tvFileSize.text = "📁 ${videoRecord.getFormattedSize()}"
                
                // Shared count
                if (videoRecord.sharedCount > 0) {
                    tvSharedCount.visibility = View.VISIBLE
                    tvSharedCount.text = "✓ Shared with ${videoRecord.sharedCount} contact${if (videoRecord.sharedCount != 1) "s" else ""}"
                } else {
                    tvSharedCount.visibility = View.GONE
                }
                
                // Cloud icon
                if (videoRecord.uploadedToCloud) {
                    ivCloudIcon.visibility = View.VISIBLE
                    ivCloudIcon.setImageResource(android.R.drawable.ic_menu_upload)
                    ivCloudIcon.setColorFilter(getColor(R.color.pulse_primary))
                } else {
                    ivCloudIcon.visibility = View.GONE
                }
                
                // Play button
                btnPlay.setOnClickListener {
                    playVideo(videoRecord)
                }
                
                // Delete button
                btnDelete.setOnClickListener {
                    confirmDelete(videoRecord)
                }
                
                // Share button
                btnShare.setOnClickListener {
                    shareVideo(videoRecord)
                }
                
                // Info button
                btnInfo.setOnClickListener {
                    showVideoInfo(videoRecord)
                }
            }
        }
    }
    
    /**
     * Play video
     */
    private fun playVideo(videoRecord: VideoRecord) {
        try {
            val videoFile = File(videoRecord.filePath)
            
            if (!videoFile.exists()) {
                Toast.makeText(this, "Video file not found", Toast.LENGTH_LONG).show()
                return
            }
            
            val videoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                videoFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Play Video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to play video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Share video
     */
    private fun shareVideo(videoRecord: VideoRecord) {
        try {
            val videoFile = File(videoRecord.filePath)
            
            if (!videoFile.exists()) {
                Toast.makeText(this, "Video file not found", Toast.LENGTH_LONG).show()
                return
            }
            
            val videoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                videoFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                putExtra(Intent.EXTRA_SUBJECT, "SOS Video Evidence")
                putExtra(Intent.EXTRA_TEXT, """
                    PulseCheck SOS Video Evidence
                    
                    Recorded: ${videoRecord.recordedAt}
                    Duration: ${videoRecord.getFormattedDuration()}
                    Size: ${videoRecord.getFormattedSize()}
                    
                    This video was automatically recorded during an emergency alert.
                """.trimIndent())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Share Video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show detailed video information
     */
    private fun showVideoInfo(videoRecord: VideoRecord) {
        val info = StringBuilder()
        info.append("📹 Video Information\n\n")
        info.append("File: ${videoRecord.fileName}\n")
        info.append("Recorded: ${videoRecord.recordedAt}\n")
        info.append("Duration: ${videoRecord.getFormattedDuration()}\n")
        info.append("Size: ${videoRecord.getFormattedSize()}\n")
        info.append("Shared: ${videoRecord.sharedCount} time${if (videoRecord.sharedCount != 1) "s" else ""}\n")
        info.append("Cloud Backup: ${if (videoRecord.uploadedToCloud) "✓ Yes" else "✗ No"}\n")
        
        if (videoRecord.cloudUrl != null) {
            info.append("\nCloud URL:\n${videoRecord.cloudUrl}\n")
        }
        
        if (videoRecord.alertId != null) {
            info.append("\nLinked to Alert #${videoRecord.alertId}\n")
        }
        
        if (videoRecord.notes != null) {
            info.append("\nNotes:\n${videoRecord.notes}\n")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Video Details")
            .setMessage(info.toString())
            .setPositiveButton("Close", null)
            .setNeutralButton("Copy URL") { _, _ ->
                if (videoRecord.cloudUrl != null) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Video URL", videoRecord.cloudUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
    
    /**
     * Confirm and delete video
     */
    private fun confirmDelete(videoRecord: VideoRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete Video?")
            .setMessage("This will permanently delete:\n${videoRecord.fileName}\n\n" +
                       if (videoRecord.uploadedToCloud) 
                           "Note: This video is backed up in the cloud." 
                       else 
                           "⚠️ Warning: This video is NOT backed up!")
            .setPositiveButton("Delete") { _, _ ->
                deleteVideo(videoRecord)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Delete video from database and file system
     */
    private fun deleteVideo(videoRecord: VideoRecord) {
        try {
            // Delete from database
            val deleted = videoDb.deleteVideo(videoRecord.id)
            
            if (deleted) {
                // Delete physical file
                val videoFile = File(videoRecord.filePath)
                if (videoFile.exists()) {
                    videoFile.delete()
                }
                
                Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show()
                loadVideos()
                updateStatistics()
                
                // TODO: Optionally delete from cloud if uploaded
                if (videoRecord.uploadedToCloud && videoRecord.cloudUrl != null) {
                    // Could add cloud deletion here
                    android.util.Log.d("VideoGallery", 
                        "Video deleted locally but still in cloud: ${videoRecord.cloudUrl}")
                }
            } else {
                Toast.makeText(this, "Failed to delete from database", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoGallery", "Error deleting video", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoDb.close()
    }
}
