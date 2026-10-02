package com.example.pulsecheck

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ContactDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATE_TABLE)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_CONTACTS ADD COLUMN $COL_EMAIL TEXT DEFAULT ''")
        }
    }

    fun insertContact(
        name: String, affiliation: String, phone: String, email: String?,
        gender: String?, homeAddress: String?, workAddress: String?,
        otherAddress: String?, avatarRes: Int
    ): Long {
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_AFFILIATION, affiliation)
            put(COL_PHONE, phone)
            put(COL_EMAIL, email ?: "")
            put(COL_GENDER, gender)
            put(COL_HOME_ADDRESS, homeAddress)
            put(COL_WORK_ADDRESS, workAddress)
            put(COL_OTHER_ADDRESS, otherAddress)
            put(COL_AVATAR_RES, avatarRes)
        }
        return getWritableDatabase().use { it.insert(TABLE_CONTACTS, null, values) }
    }

    fun insertContact(
        name: String, affiliation: String, phone: String, gender: String?,
        homeAddress: String?, workAddress: String?, otherAddress: String?, avatarRes: Int
    ) = insertContact(name, affiliation, phone, "", gender, homeAddress, workAddress, otherAddress, avatarRes)

    fun getAllContacts(): List<contact> {
        val contacts = mutableListOf<contact>()
        getReadableDatabase().use { db ->
            db.query(TABLE_CONTACTS, null, null, null, null, null, "$COL_NAME ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    var avatar = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AVATAR_RES))
                    if (avatar == 0) avatar = R.drawable.person1
                    contacts += contact(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_AFFILIATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_HOME_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WORK_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_OTHER_ADDRESS)),
                        avatar
                    )
                }
            }
        }
        return contacts
    }

    fun getAllPhoneNumbers() = getStringColumn(COL_PHONE)
    fun getAllEmails() = getStringColumn(COL_EMAIL)

    private fun getStringColumn(column: String): List<String> {
        val values = mutableListOf<String>()
        getReadableDatabase().use { db ->
            db.query(TABLE_CONTACTS, arrayOf(column), null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }?.let(values::add)
                }
            }
        }
        return values
    }

    fun updateAvatar(name: String, phone: String, avatarRes: Int): Int {
        val values = ContentValues().apply { put(COL_AVATAR_RES, avatarRes) }
        return getWritableDatabase().use {
            it.update(TABLE_CONTACTS, values, "$COL_NAME=? AND $COL_PHONE=?", arrayOf(name, phone))
        }
    }

    fun updateContact(
        id: Int, name: String, affiliation: String?, phone: String?, email: String?,
        gender: String?, homeAddress: String?, workAddress: String?,
        otherAddress: String?, avatarRes: Int
    ): Int {
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_AFFILIATION, affiliation ?: "")
            put(COL_PHONE, phone ?: "")
            put(COL_EMAIL, email ?: "")
            put(COL_GENDER, gender ?: "")
            put(COL_HOME_ADDRESS, homeAddress ?: "")
            put(COL_WORK_ADDRESS, workAddress ?: "")
            put(COL_OTHER_ADDRESS, otherAddress ?: "")
            put(COL_AVATAR_RES, avatarRes)
        }
        return getWritableDatabase().use { it.update(TABLE_CONTACTS, values, "$COL_ID=?", arrayOf(id.toString())) }
    }

    fun getContactById(id: Int): Array<String>? {
        getReadableDatabase().use { db ->
            db.query(TABLE_CONTACTS, null, "$COL_ID=?", arrayOf(id.toString()), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    return arrayOf(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)).toString(),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_AFFILIATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_HOME_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WORK_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_OTHER_ADDRESS)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_AVATAR_RES)).toString()
                    )
                }
            }
        }
        return null
    }

    fun deleteContact(name: String, phone: String): Int =
        getWritableDatabase().use { it.delete(TABLE_CONTACTS, "$COL_NAME=? AND $COL_PHONE=?", arrayOf(name, phone)) }

    fun getContactCount(): Int =
        getReadableDatabase().use { db ->
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACTS", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        }

    companion object {
        private const val DATABASE_NAME = "pulsecheck_contacts.db"
        private const val DATABASE_VERSION = 2
        const val TABLE_CONTACTS = "contacts"
        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_AFFILIATION = "affiliation"
        const val COL_PHONE = "phone"
        const val COL_EMAIL = "email"
        const val COL_GENDER = "gender"
        const val COL_HOME_ADDRESS = "home_address"
        const val COL_WORK_ADDRESS = "work_address"
        const val COL_OTHER_ADDRESS = "other_address"
        const val COL_AVATAR_RES = "avatar_res"
        private val CREATE_TABLE = "CREATE TABLE $TABLE_CONTACTS (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NAME TEXT NOT NULL, " +
            "$COL_AFFILIATION TEXT, $COL_PHONE TEXT NOT NULL, $COL_EMAIL TEXT DEFAULT '', " +
            "$COL_GENDER TEXT, $COL_HOME_ADDRESS TEXT, $COL_WORK_ADDRESS TEXT, " +
            "$COL_OTHER_ADDRESS TEXT, $COL_AVATAR_RES INTEGER);"
    }
}


