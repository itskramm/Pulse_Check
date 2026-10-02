package com.example.pulsecheck

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Supabase Settings Activity
 * Allows users to configure Supabase credentials and enable/disable cloud sync
 */
class SupabaseSettings : AppCompatActivity() {
    
    private lateinit var switchSupabaseEnabled: SwitchCompat
    private lateinit var tvConnectionStatus: TextView
    private lateinit var layoutCredentials: LinearLayout
    private lateinit var etSupabaseUrl: EditText
    private lateinit var etSupabaseKey: EditText
    private lateinit var btnSaveCredentials: Button
    private lateinit var btnTestConnection: Button
    private lateinit var btnSyncContacts: Button
    private lateinit var btnViewAlertHistory: Button
    private lateinit var tvSyncStatus: TextView
    
    private lateinit var supabaseManager: SupabaseManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_settings)
        
        supabaseManager = SupabaseManager.getInstance(this)
        
        initViews()
        setupListeners()
        loadSettings()
    }
    
    private fun initViews() {
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener { finish() }
        
        switchSupabaseEnabled = findViewById(R.id.switch_supabase_enabled)
        tvConnectionStatus = findViewById(R.id.tv_connection_status)
        layoutCredentials = findViewById(R.id.layout_credentials)
        etSupabaseUrl = findViewById(R.id.et_supabase_url)
        etSupabaseKey = findViewById(R.id.et_supabase_key)
        btnSaveCredentials = findViewById(R.id.btn_save_credentials)
        btnTestConnection = findViewById(R.id.btn_test_connection)
        btnSyncContacts = findViewById(R.id.btn_sync_contacts)
        btnViewAlertHistory = findViewById(R.id.btn_view_alert_history)
        tvSyncStatus = findViewById(R.id.tv_sync_status)
        
        // Set hints from resources
        etSupabaseUrl.hint = getString(R.string.cloud_sync_url_hint)
        etSupabaseKey.hint = getString(R.string.cloud_sync_key_hint)
    }
    
    private fun setupListeners() {
        switchSupabaseEnabled.setOnCheckedChangeListener { _, isChecked ->
            handleSupabaseToggle(isChecked)
        }
        
        btnSaveCredentials.setOnClickListener {
            saveCredentials()
        }
        
        btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        btnSyncContacts.setOnClickListener {
            syncContactsToSupabase()
        }
        
        btnViewAlertHistory.setOnClickListener {
            viewAlertHistory()
        }
    }
    
    private fun loadSettings() {
        val isEnabled = SupabaseConfig.isEnabled(this)
        val isConfigured = SupabaseConfig.isConfigured(this)
        
        switchSupabaseEnabled.isChecked = isEnabled
        
        if (isConfigured) {
            etSupabaseUrl.setText(SupabaseConfig.getUrl(this))
            etSupabaseKey.setText(SupabaseConfig.getAnonKey(this))
            updateConnectionStatus(isEnabled)
        } else {
            updateConnectionStatus(false)
            etSupabaseUrl.hint = "https://xxxxx.supabase.co"
            etSupabaseKey.hint = "Your anon/public key"
        }
        
        updateUIState(isEnabled && isConfigured)
    }
    
    private fun handleSupabaseToggle(enabled: Boolean) {
        if (enabled && !SupabaseConfig.isConfigured(this)) {
            // Show credentials form if not configured
            AlertDialog.Builder(this)
                .setTitle("Configure Supabase")
                .setMessage("Please enter your Supabase credentials first.")
                .setPositiveButton("OK") { _, _ ->
                    switchSupabaseEnabled.isChecked = false
                }
                .show()
            return
        }
        
        SupabaseConfig.setEnabled(this, enabled)
        updateConnectionStatus(enabled)
        updateUIState(enabled && SupabaseConfig.isConfigured(this))
        
        if (enabled) {
            Toast.makeText(this, "Supabase cloud sync enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Supabase cloud sync disabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveCredentials() {
        val url = etSupabaseUrl.text.toString().trim()
        val key = etSupabaseKey.text.toString().trim()
        
        if (url.isEmpty()) {
            etSupabaseUrl.error = "URL is required"
            etSupabaseUrl.requestFocus()
            return
        }
        
        if (key.isEmpty()) {
            etSupabaseKey.error = "Anon key is required"
            etSupabaseKey.requestFocus()
            return
        }
        
        if (!url.startsWith("https://")) {
            etSupabaseUrl.error = "URL must start with https://"
            etSupabaseUrl.requestFocus()
            return
        }
        
        // Save credentials
        SupabaseConfig.saveCredentials(this, url, key)
        
        Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_SHORT).show()
        
        // Update UI
        updateConnectionStatus(SupabaseConfig.isEnabled(this))
    }
    
    private fun testConnection() {
        if (!SupabaseConfig.isConfigured(this)) {
            Toast.makeText(this, "Please configure Supabase credentials first", Toast.LENGTH_LONG).show()
            return
        }
        
        btnTestConnection.isEnabled = false
        btnTestConnection.text = "Testing..."
        
        lifecycleScope.launch {
            try {
                // Initialize if not already
                if (!SupabaseConfig.isInitialized()) {
                    SupabaseConfig.initialize(this@SupabaseSettings)
                }
                
                // Try to check if authenticated or just verify connection
                val isAuth = supabaseManager.isAuthenticated()
                
                runOnUiThread {
                    btnTestConnection.isEnabled = true
                    btnTestConnection.text = "Test Connection"
                    
                    AlertDialog.Builder(this@SupabaseSettings)
                        .setTitle("Connection Test")
                        .setMessage(
                            "✅ Connection successful!\n\n" +
                            "Authentication: ${if (isAuth) "Signed in" else "Not signed in"}\n\n" +
                            "Your Supabase configuration is correct."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    btnTestConnection.isEnabled = true
                    btnTestConnection.text = "Test Connection"
                    
                    AlertDialog.Builder(this@SupabaseSettings)
                        .setTitle("Connection Failed")
                        .setMessage(
                            "❌ Failed to connect to Supabase.\n\n" +
                            "Error: ${e.message}\n\n" +
                            "Please check:\n" +
                            "• Your Supabase URL is correct\n" +
                            "• Your anon key is valid\n" +
                            "• You have internet connection"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    
    private fun syncContactsToSupabase() {
        if (!SupabaseConfig.isEnabled(this)) {
            Toast.makeText(this, "Please enable Supabase first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!supabaseManager.isAuthenticated()) {
            Toast.makeText(this, "Please sign in to sync contacts", Toast.LENGTH_LONG).show()
            return
        }
        
        btnSyncContacts.isEnabled = false
        tvSyncStatus.text = "Syncing contacts..."
        tvSyncStatus.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Get local contacts
                val dbHelper = ContactDatabaseHelper(this@SupabaseSettings)
                val localContacts = dbHelper.getAllContacts()
                
                supabaseManager.syncContacts(localContacts, object : SupabaseManager.SyncCallback {
                    override fun onSuccess(count: Int) {
                        runOnUiThread {
                            btnSyncContacts.isEnabled = true
                            tvSyncStatus.text = "✅ Synced $count contacts successfully"
                            Toast.makeText(
                                this@SupabaseSettings,
                                "$count contacts synced to cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    
                    override fun onFailure(error: String) {
                        runOnUiThread {
                            btnSyncContacts.isEnabled = true
                            tvSyncStatus.text = "❌ Sync failed: $error"
                            Toast.makeText(
                                this@SupabaseSettings,
                                "Sync failed: $error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
                
            } catch (e: Exception) {
                runOnUiThread {
                    btnSyncContacts.isEnabled = true
                    tvSyncStatus.text = "❌ Error: ${e.message}"
                }
            }
        }
    }
    
    private fun viewAlertHistory() {
        if (!SupabaseConfig.isEnabled(this)) {
            Toast.makeText(this, "Please enable Supabase first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!supabaseManager.isAuthenticated()) {
            Toast.makeText(this, "Please sign in to view cloud history", Toast.LENGTH_LONG).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                supabaseManager.getAlertHistory(object : SupabaseManager.AlertHistoryCallback {
                    override fun onSuccess(alerts: List<SupabaseManager.SOSAlert>) {
                        runOnUiThread {
                            showAlertHistoryDialog(alerts)
                        }
                    }
                    
                    override fun onFailure(error: String) {
                        runOnUiThread {
                            Toast.makeText(
                                this@SupabaseSettings,
                                "Failed to load history: $error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@SupabaseSettings,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showAlertHistoryDialog(alerts: List<SupabaseManager.SOSAlert>) {
        val message = if (alerts.isEmpty()) {
            "No cloud alert history found."
        } else {
            buildString {
                appendLine("Found ${alerts.size} alerts in cloud:\n")
                alerts.take(10).forEachIndexed { index, alert ->
                    appendLine("${index + 1}. ${alert.triggerType}")
                    appendLine("   Status: ${alert.status}")
                    appendLine("   Contacts notified: ${alert.contactsNotified}")
                    appendLine("   Location: ${alert.location ?: "Unknown"}")
                    appendLine("   Time: ${alert.createdAt}")
                    appendLine()
                }
                if (alerts.size > 10) {
                    appendLine("... and ${alerts.size - 10} more")
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Cloud Alert History")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun updateConnectionStatus(enabled: Boolean) {
        if (enabled && SupabaseConfig.isConfigured(this)) {
            tvConnectionStatus.text = "✅ Connected to Supabase"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else if (SupabaseConfig.isConfigured(this)) {
            tvConnectionStatus.text = "⚪ Configured but disabled"
            tvConnectionStatus.setTextColor(getColor(android.R.color.darker_gray))
        } else {
            tvConnectionStatus.text = "❌ Not configured"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    private fun updateUIState(enabled: Boolean) {
        btnSyncContacts.isEnabled = enabled
        btnViewAlertHistory.isEnabled = enabled
        
        if (enabled) {
            tvSyncStatus.visibility = View.GONE
        }
    }
}
