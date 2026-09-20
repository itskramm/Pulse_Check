package com.example.pulsecheck

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertHistoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATE_TABLE)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_HISTORY ADD COLUMN $COL_PATH_DISTANCE REAL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_HISTORY ADD COLUMN $COL_PATH_DURATION INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_HISTORY ADD COLUMN $COL_PATH_POINTS INTEGER DEFAULT 0")
        }
    }

    fun logAlert(triggerType: String, contactsSent: Int, status: String, location: String?): Long =
        logAlert(triggerType, contactsSent, status, location, 0.0, 0L, 0)

    fun logAlert(
        triggerType: String,
        contactsSent: Int,
        status: String,
        location: String?,
        pathDistance: Double,
        pathDuration: Long,
        pathPoints: Int
    ): Long {
        val now = System.currentTimeMillis()
        val date = Date(now)
        val values = ContentValues().apply {
            put(COL_TRIGGER_TYPE, triggerType)
            put(COL_TIMESTAMP, now)
            put(COL_DATE_DISPLAY, SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date))
            put(COL_TIME_DISPLAY, SimpleDateFormat("h:mm a", Locale.getDefault()).format(date))
            put(COL_CONTACTS_SENT, contactsSent)
            put(COL_STATUS, status)
            put(COL_LOCATION, location ?: "Unknown")
            put(COL_PATH_DISTANCE, pathDistance)
            put(COL_PATH_DURATION, pathDuration)
            put(COL_PATH_POINTS, pathPoints)
        }
        return getWritableDatabase().use { it.insert(TABLE_HISTORY, null, values) }
    }

    fun getAllHistory(): List<AlertHistoryItem> {
        val items = mutableListOf<AlertHistoryItem>()
        getReadableDatabase().use { db ->
            db.query(TABLE_HISTORY, null, null, null, null, null, "$COL_TIMESTAMP DESC").use { cursor ->
                while (cursor.moveToNext()) {
                    val distanceIndex = cursor.getColumnIndex(COL_PATH_DISTANCE)
                    val durationIndex = cursor.getColumnIndex(COL_PATH_DURATION)
                    val pointsIndex = cursor.getColumnIndex(COL_PATH_POINTS)
                    items += AlertHistoryItem(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIGGER_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE_DISPLAY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME_DISPLAY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CONTACTS_SENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
                        if (distanceIndex >= 0) cursor.getDouble(distanceIndex) else 0.0,
                        if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L,
                        if (pointsIndex >= 0) cursor.getInt(pointsIndex) else 0
                    )
                }
            }
        }
        return items
    }

    fun clearHistory() {
        getWritableDatabase().use { it.delete(TABLE_HISTORY, null, null) }
    }

    fun getTotalAlertCount(): Int =
        getReadableDatabase().use { db ->
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_HISTORY", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        }

    companion object {
        private const val DATABASE_NAME = "pulsecheck_history.db"
        private const val DATABASE_VERSION = 2
        const val TABLE_HISTORY = "alert_history"
        const val COL_ID = "_id"
        const val COL_TRIGGER_TYPE = "trigger_type"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_DATE_DISPLAY = "date_display"
        const val COL_TIME_DISPLAY = "time_display"
        const val COL_CONTACTS_SENT = "contacts_sent"
        const val COL_STATUS = "status"
        const val COL_LOCATION = "location"
        const val COL_PATH_DISTANCE = "path_distance"
        const val COL_PATH_DURATION = "path_duration"
        const val COL_PATH_POINTS = "path_points"
        private val CREATE_TABLE = "CREATE TABLE $TABLE_HISTORY (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TRIGGER_TYPE TEXT NOT NULL, " +
            "$COL_TIMESTAMP INTEGER NOT NULL, $COL_DATE_DISPLAY TEXT NOT NULL, " +
            "$COL_TIME_DISPLAY TEXT NOT NULL, $COL_CONTACTS_SENT INTEGER DEFAULT 0, " +
            "$COL_STATUS TEXT NOT NULL, $COL_LOCATION TEXT DEFAULT 'Unknown', " +
            "$COL_PATH_DISTANCE REAL DEFAULT 0, $COL_PATH_DURATION INTEGER DEFAULT 0, " +
            "$COL_PATH_POINTS INTEGER DEFAULT 0);"
    }
}
