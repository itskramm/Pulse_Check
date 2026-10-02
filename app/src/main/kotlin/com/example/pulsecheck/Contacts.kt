package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Contacts : AppCompatActivity() {
    private lateinit var dbHelper: ContactDatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private val contacts = mutableListOf<contact>()
    private lateinit var adapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        dbHelper = ContactDatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewContacts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactAdapter(contacts, { item, position ->
            dbHelper.deleteContact(item.getName(), item.getNumber())
            contacts.removeAt(position)
            adapter.notifyItemRemoved(position)
        }, dbHelper)
        recyclerView.adapter = adapter
        findViewById<ImageView>(R.id.icon_settings)?.setOnClickListener { }
        findViewById<ImageView>(R.id.icon_notification)?.setOnClickListener { }
        findViewById<View>(R.id.btn_add_new_contact)?.setOnClickListener {
            startActivity(Intent(this, add_contact::class.java))
        }
        findViewById<EditText>(R.id.et_search)?.setOnEditorActionListener { _, _, _ ->
            filterContacts()
            false
        }
        NavHelper.setup(this, NavHelper.TAB_CONTACTS)
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

    private fun loadContacts() {
        contacts.clear()
        contacts.addAll(dbHelper.getAllContacts())
        adapter.notifyDataSetChanged()
    }

    private fun filterContacts() {
        val query = findViewById<EditText>(R.id.et_search).text.toString().trim().lowercase()
        val filtered = dbHelper.getAllContacts().filter {
            it.getName().lowercase().contains(query) || it.getNumber().contains(query)
        }
        contacts.clear()
        contacts.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}
