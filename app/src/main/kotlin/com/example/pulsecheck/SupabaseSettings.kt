package com.example.pulsecheck

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SupabaseSettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_settings)
        val enabled = findViewById<Switch>(R.id.switch_supabase_enabled)
        val status = findViewById<TextView>(R.id.tv_connection_status)
        val url = findViewById<EditText>(R.id.et_supabase_url)
        val key = findViewById<EditText>(R.id.et_supabase_key)
        enabled.isChecked = SupabaseConfig.isEnabled(this)
        url.setText(SupabaseConfig.getUrl(this))
        key.setText(SupabaseConfig.getAnonKey(this))
        status.text = if (SupabaseConfig.isConfigured(this)) "Configured" else "Not configured"
        enabled.setOnCheckedChangeListener { _, checked -> SupabaseConfig.setEnabled(this, checked) }
        findViewById<Button>(R.id.btn_save_credentials).setOnClickListener {
            SupabaseConfig.saveCredentials(this, url.text.toString().trim(), key.text.toString().trim())
            status.text = "Saved"
            Toast.makeText(this, "Supabase settings saved", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_test_connection).setOnClickListener {
            Toast.makeText(this, "Connection testing is unavailable in this build.", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_back)?.setOnClickListener { finish() }
    }
}
