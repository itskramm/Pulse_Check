package com.example.pulsecheck

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATE_TABLE)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun addNotification(type: String, message: String) {
        val time = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(COL_TYPE, type)
            put(COL_MESSAGE, message)
            put(COL_TIME, time)
        }
        getWritableDatabase().use { it.insert(TABLE, null, values) }
    }

    fun getAll(): List<NotificationItem> {
        val list = mutableListOf<NotificationItem>()
        getReadableDatabase().use { db ->
            db.query(TABLE, null, null, null, null, null, "$COL_ID DESC").use { cursor ->
                while (cursor.moveToNext()) {
                    list += NotificationItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME))
                    )
                }
            }
        }
        return list
    }

    fun getCount(): Int =
        getReadableDatabase().use { db ->
            db.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        }

    fun clearAll() {
        getWritableDatabase().use { it.delete(TABLE, null, null) }
    }

    class NotificationItem(
        @JvmField val id: Int,
        @JvmField val type: String,
        @JvmField val message: String,
        @JvmField val time: String
    )

    companion object {
        private const val DB_NAME = "pulsecheck_notifications.db"
        private const val DB_VERSION = 1
        const val TABLE = "notifications"
        const val COL_ID = "_id"
        const val COL_TYPE = "type"
        const val COL_MESSAGE = "message"
        const val COL_TIME = "time"
        const val TYPE_CONTACT_ADDED = "contact_added"
        const val TYPE_SOS_SENT = "sos_sent"
        private val CREATE_TABLE = "CREATE TABLE $TABLE (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COL_TYPE TEXT NOT NULL, $COL_MESSAGE TEXT NOT NULL, $COL_TIME TEXT NOT NULL);"
    }
}
