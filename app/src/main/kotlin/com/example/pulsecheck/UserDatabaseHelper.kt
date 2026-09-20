package com.example.pulsecheck

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATE_TABLE)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_GENDER TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_PERM_ADDRESS TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_WORK_ADDRESS TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_OTHER_ADDRESSES TEXT DEFAULT ''")
        }
        if (oldVersion < 3) db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_EMAIL TEXT DEFAULT ''")
    }

    fun registerUser(fullName: String, phone: String, email: String, password: String): Boolean {
        if (phoneExists(phone)) return false
        val salt = generateSalt()
        val values = ContentValues().apply {
            put(COL_FULL_NAME, fullName)
            put(COL_PHONE, phone)
            put(COL_EMAIL, email)
            put(COL_PASSWORD_HASH, hashPassword(password, salt))
            put(COL_SALT, salt)
        }
        return getWritableDatabase().use { it.insert(TABLE_USERS, null, values) != -1L }
    }

    fun loginUser(phone: String, password: String): String? {
        getReadableDatabase().use { db ->
            db.query(TABLE_USERS, arrayOf(COL_FULL_NAME, COL_PASSWORD_HASH, COL_SALT),
                "$COL_PHONE=?", arrayOf(phone), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val stored = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_HASH))
                    val salt = cursor.getString(cursor.getColumnIndexOrThrow(COL_SALT))
                    return cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_NAME))
                        .takeIf { hashPassword(password, salt) == stored }
                }
            }
        }
        return null
    }

    fun phoneExists(phone: String): Boolean =
        getReadableDatabase().use { db ->
            db.query(TABLE_USERS, arrayOf(COL_ID), "$COL_PHONE=?", arrayOf(phone), null, null, null).use {
                it.count > 0
            }
        }

    fun updateDisplayName(phone: String, newName: String): Boolean {
        val values = ContentValues().apply { put(COL_FULL_NAME, newName) }
        return getWritableDatabase().use { it.update(TABLE_USERS, values, "$COL_PHONE=?", arrayOf(phone)) > 0 }
    }

    fun updateProfile(
        phone: String, displayName: String?, email: String?, gender: String?,
        permAddress: String?, workAddress: String?, otherAddresses: String?
    ): Boolean {
        val values = ContentValues().apply {
            put(COL_FULL_NAME, displayName ?: "")
            put(COL_EMAIL, email ?: "")
            put(COL_GENDER, gender ?: "")
            put(COL_PERM_ADDRESS, permAddress ?: "")
            put(COL_WORK_ADDRESS, workAddress ?: "")
            put(COL_OTHER_ADDRESSES, otherAddresses ?: "")
        }
        return getWritableDatabase().use { it.update(TABLE_USERS, values, "$COL_PHONE=?", arrayOf(phone)) > 0 }
    }

    fun getProfile(phone: String): Array<String> {
        val result = arrayOf("", "", "", "", "", "")
        getReadableDatabase().use { db ->
            db.query(TABLE_USERS, arrayOf(COL_FULL_NAME, COL_EMAIL, COL_GENDER, COL_PERM_ADDRESS, COL_WORK_ADDRESS, COL_OTHER_ADDRESSES),
                "$COL_PHONE=?", arrayOf(phone), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) for (i in result.indices) result[i] = cursor.getString(i) ?: ""
            }
        }
        return result
    }

    fun getFullName(phone: String): String =
        getReadableDatabase().use { db ->
            db.query(TABLE_USERS, arrayOf(COL_FULL_NAME), "$COL_PHONE=?", arrayOf(phone), null, null, null).use {
                if (it.moveToFirst()) it.getString(0) else "User"
            }
        }

    fun resetPassword(phone: String, newPassword: String): Boolean {
        val salt = generateSalt()
        val values = ContentValues().apply {
            put(COL_PASSWORD_HASH, hashPassword(newPassword, salt))
            put(COL_SALT, salt)
        }
        return getWritableDatabase().use { it.update(TABLE_USERS, values, "$COL_PHONE=?", arrayOf(phone)) > 0 }
    }

    private fun generateSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
    private fun hashPassword(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray(StandardCharsets.UTF_8)).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    companion object {
        private const val DATABASE_NAME = "pulsecheck_users.db"
        private const val DATABASE_VERSION = 3
        const val TABLE_USERS = "users"
        const val COL_ID = "_id"
        const val COL_FULL_NAME = "full_name"
        const val COL_PHONE = "phone"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD_HASH = "password_hash"
        const val COL_SALT = "salt"
        const val COL_GENDER = "gender"
        const val COL_PERM_ADDRESS = "permanent_address"
        const val COL_WORK_ADDRESS = "work_address"
        const val COL_OTHER_ADDRESSES = "other_addresses"
        private val CREATE_TABLE = "CREATE TABLE $TABLE_USERS (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FULL_NAME TEXT NOT NULL, " +
            "$COL_PHONE TEXT NOT NULL UNIQUE, $COL_EMAIL TEXT DEFAULT '', " +
            "$COL_PASSWORD_HASH TEXT NOT NULL, $COL_SALT TEXT NOT NULL, $COL_GENDER TEXT DEFAULT '', " +
            "$COL_PERM_ADDRESS TEXT DEFAULT '', $COL_WORK_ADDRESS TEXT DEFAULT '', " +
            "$COL_OTHER_ADDRESSES TEXT DEFAULT '');"
    }
}
