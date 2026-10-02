package com.example.pulsecheck

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Database helper for storing video recording metadata
 * 
 * Stores:
 * - Video file paths
 * - Recording timestamps
 * - Duration
 * - File size
 * - Associated SOS alert ID
 * - Sync status (uploaded to cloud or not)
 */
class VideoDatabaseHelper(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "videos.db"
        private const val DATABASE_VERSION = 1
        
        // Table name
        private const val TABLE_VIDEOS = "videos"
        
        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_FILE_PATH = "file_path"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_DURATION_MS = "duration_ms"
        private const val COLUMN_FILE_SIZE_BYTES = "file_size_bytes"
        private const val COLUMN_RECORDED_AT = "recorded_at"
        private const val COLUMN_ALERT_ID = "alert_id"
        private const val COLUMN_SHARED_COUNT = "shared_count"
        private const val COLUMN_UPLOADED_TO_CLOUD = "uploaded_to_cloud"
        private const val COLUMN_CLOUD_URL = "cloud_url"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_IS_ENCRYPTED = "is_encrypted"
        private const val COLUMN_ENCRYPTION_IV = "encryption_iv"
        private const val COLUMN_VIEWABLE_BY_USER = "viewable_by_user"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_VIDEOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FILE_PATH TEXT NOT NULL,
                $COLUMN_FILE_NAME TEXT NOT NULL,
                $COLUMN_DURATION_MS INTEGER DEFAULT 0,
                $COLUMN_FILE_SIZE_BYTES INTEGER DEFAULT 0,
                $COLUMN_RECORDED_AT TEXT NOT NULL,
                $COLUMN_ALERT_ID INTEGER DEFAULT NULL,
                $COLUMN_SHARED_COUNT INTEGER DEFAULT 0,
                $COLUMN_UPLOADED_TO_CLOUD INTEGER DEFAULT 0,
                $COLUMN_CLOUD_URL TEXT DEFAULT NULL,
                $COLUMN_NOTES TEXT DEFAULT NULL,
                $COLUMN_IS_ENCRYPTED INTEGER DEFAULT 1,
                $COLUMN_ENCRYPTION_IV TEXT DEFAULT NULL,
                $COLUMN_VIEWABLE_BY_USER INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VIDEOS")
        onCreate(db)
    }

    /**
     * Add a new video record to database (encrypted, not viewable by default)
     * 
     * @param filePath Full path to encrypted video file
     * @param fileName Original video file name (before encryption)
     * @param durationMs Recording duration in milliseconds
     * @param fileSizeBytes File size in bytes
     * @param alertId Associated alert history ID (optional)
     * @param encryptionIV Encryption initialization vector
     * @param viewableByUser Whether user can view (default: false for security)
     * @return Video record ID
     */
    fun addVideo(
        filePath: String,
        fileName: String,
        durationMs: Long = 0,
        fileSizeBytes: Long = 0,
        alertId: Int? = null,
        encryptionIV: String? = null,
        viewableByUser: Boolean = false
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FILE_PATH, filePath)
            put(COLUMN_FILE_NAME, fileName)
            put(COLUMN_DURATION_MS, durationMs)
            put(COLUMN_FILE_SIZE_BYTES, fileSizeBytes)
            put(COLUMN_RECORDED_AT, getCurrentTimestamp())
            if (alertId != null) {
                put(COLUMN_ALERT_ID, alertId)
            }
            put(COLUMN_SHARED_COUNT, 0)
            put(COLUMN_UPLOADED_TO_CLOUD, 0)
            put(COLUMN_IS_ENCRYPTED, 1) // Always encrypted
            put(COLUMN_ENCRYPTION_IV, encryptionIV)
            put(COLUMN_VIEWABLE_BY_USER, if (viewableByUser) 1 else 0)
        }
        
        val id = db.insert(TABLE_VIDEOS, null, values)
        android.util.Log.d("VideoDB", "Added encrypted video: $fileName (ID: $id, Viewable: $viewableByUser)")
        return id
    }

    /**
     * Update video record after sharing
     */
    fun updateVideoShared(videoId: Long, sharedCount: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SHARED_COUNT, sharedCount)
        }
        db.update(TABLE_VIDEOS, values, "$COLUMN_ID = ?", arrayOf(videoId.toString()))
        android.util.Log.d("VideoDB", "Updated video $videoId: shared with $sharedCount contacts")
    }

    /**
     * Update video record after cloud upload
     */
    fun updateVideoCloudStatus(videoId: Long, cloudUrl: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UPLOADED_TO_CLOUD, 1)
            put(COLUMN_CLOUD_URL, cloudUrl)
        }
        db.update(TABLE_VIDEOS, values, "$COLUMN_ID = ?", arrayOf(videoId.toString()))
        android.util.Log.d("VideoDB", "Updated video $videoId: uploaded to $cloudUrl")
    }

    /**
     * Get all videos ordered by newest first
     */
    fun getAllVideos(): List<VideoRecord> {
        val videos = mutableListOf<VideoRecord>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_VIDEOS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ID DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                videos.add(cursorToVideoRecord(this))
            }
            close()
        }

        return videos
    }

    /**
     * Get videos that haven't been uploaded to cloud
     */
    fun getUnuploadedVideos(): List<VideoRecord> {
        val videos = mutableListOf<VideoRecord>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_VIDEOS,
            null,
            "$COLUMN_UPLOADED_TO_CLOUD = ?",
            arrayOf("0"),
            null,
            null,
            "$COLUMN_ID DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                videos.add(cursorToVideoRecord(this))
            }
            close()
        }

        return videos
    }

    /**
     * Get video by ID
     */
    fun getVideoById(videoId: Long): VideoRecord? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_VIDEOS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(videoId.toString()),
            null,
            null,
            null
        )

        var video: VideoRecord? = null
        with(cursor) {
            if (moveToFirst()) {
                video = cursorToVideoRecord(this)
            }
            close()
        }

        return video
    }

    /**
     * Delete video record
     */
    fun deleteVideo(videoId: Long): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_VIDEOS, "$COLUMN_ID = ?", arrayOf(videoId.toString()))
        android.util.Log.d("VideoDB", "Deleted video $videoId: success=${result > 0}")
        return result > 0
    }

    /**
     * Get total storage used by videos
     */
    fun getTotalStorageUsed(): Long {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COLUMN_FILE_SIZE_BYTES) FROM $TABLE_VIDEOS",
            null
        )
        
        var total = 0L
        if (cursor.moveToFirst()) {
            total = cursor.getLong(0)
        }
        cursor.close()
        
        return total
    }

    /**
     * Get recent videos (used for SOS alerts)
     * @param limit Maximum number of videos to return
     * @return List of recent videos ordered by newest first
     */
    fun getRecentVideos(limit: Int = 10): List<VideoRecord> {
        val videos = mutableListOf<VideoRecord>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_VIDEOS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ID DESC",
            limit.toString()
        )

        with(cursor) {
            while (moveToNext()) {
                videos.add(cursorToVideoRecord(this))
            }
            close()
        }

        android.util.Log.d("VideoDB", "Retrieved ${videos.size} recent videos")
        return videos
    }
    
    /**
     * Link video to alert ID
     * @param videoId Video record ID
     * @param alertId Alert identifier (can be String for cloud alerts)
     */
    fun linkVideoToAlert(videoId: Long, alertId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            // Store alert ID in notes field if alertId is String
            put(COLUMN_NOTES, "alert:$alertId")
        }
        val updated = db.update(TABLE_VIDEOS, values, "$COLUMN_ID = ?", arrayOf(videoId.toString()))
        
        if (updated > 0) {
            android.util.Log.d("VideoDB", "Linked video $videoId to alert $alertId")
        } else {
            android.util.Log.w("VideoDB", "Failed to link video $videoId to alert $alertId")
        }
    }
    
    /**
     * Get videos by alert ID (from notes field)
     */
    fun getVideosByAlert(alertId: String): List<VideoRecord> {
        val videos = mutableListOf<VideoRecord>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_VIDEOS,
            null,
            "$COLUMN_NOTES LIKE ?",
            arrayOf("%alert:$alertId%"),
            null,
            null,
            "$COLUMN_ID ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                videos.add(cursorToVideoRecord(this))
            }
            close()
        }

        android.util.Log.d("VideoDB", "Found ${videos.size} videos for alert $alertId")
        return videos
    }

    /**
     * Get video count
     */
    fun getVideoCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_VIDEOS", null)
        
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        
        return count
    }

    /**
     * Convert cursor to VideoRecord object
     */
    private fun cursorToVideoRecord(cursor: Cursor): VideoRecord {
        return VideoRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            filePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_PATH)),
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
            durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION_MS)),
            fileSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE_BYTES)),
            recordedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDED_AT)),
            alertId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ALERT_ID))) null 
                     else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ALERT_ID)),
            sharedCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SHARED_COUNT)),
            uploadedToCloud = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UPLOADED_TO_CLOUD)) == 1,
            cloudUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLOUD_URL)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
            isEncrypted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ENCRYPTED)) == 1,
            encryptionIV = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCRYPTION_IV)),
            viewableByUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VIEWABLE_BY_USER)) == 1
        )
    }

    /**
     * Get current timestamp in standard format
     */
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}

/**
 * Data class representing a video record
 */
data class VideoRecord(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val recordedAt: String,
    val alertId: Int?,
    val sharedCount: Int,
    val uploadedToCloud: Boolean,
    val cloudUrl: String?,
    val notes: String?,
    val isEncrypted: Boolean = true,
    val encryptionIV: String? = null,
    val viewableByUser: Boolean = false
) {
    /**
     * Get formatted file size
     */
    fun getFormattedSize(): String {
        val kb = fileSizeBytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$fileSizeBytes bytes"
        }
    }

    /**
     * Get formatted duration
     */
    fun getFormattedDuration(): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            String.format("%d:%02d", minutes, remainingSeconds)
        } else {
            "${seconds}s"
        }
    }
    
    /**
     * Check if video is accessible to user
     */
    fun isAccessible(): Boolean {
        return viewableByUser
    }
    
    /**
     * Get security status badge
     */
    fun getSecurityBadge(): String {
        return when {
            !viewableByUser && isEncrypted -> "🔒 Secure (Admin Only)"
            !viewableByUser -> "🔒 Restricted"
            isEncrypted -> "🔐 Encrypted"
            else -> "📹 Available"
        }
    }
}
