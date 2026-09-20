package com.example.pulsecheck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class History : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var historyDb: AlertHistoryDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        historyDb = AlertHistoryDatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        findViewById<TextView>(R.id.btn_clear_history)?.setOnClickListener { confirmClearHistory() }
        findViewById<TextView>(R.id.btn_export_history)?.setOnClickListener { exportHistory() }
        NavHelper.setup(this, NavHelper.TAB_HISTORY)
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val items = historyDb.getAllHistory()
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        layoutEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        if (items.isNotEmpty()) recyclerView.adapter = AlertHistoryAdapter(items)
    }

    private fun confirmClearHistory() {
        if (historyDb.getTotalAlertCount() == 0) {
            Toast.makeText(this, "No history to clear", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Clear Alert History")
            .setMessage("This will permanently delete all SOS alert records. Continue?")
            .setPositiveButton("Clear All") { _, _ ->
                historyDb.clearHistory()
                loadHistory()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun exportHistory() {
        val items = historyDb.getAllHistory()
        if (items.isEmpty()) {
            Toast.makeText(this, "No history to export", Toast.LENGTH_SHORT).show()
            return
        }
        val report = buildString {
            appendLine("PulseCheck — SOS Alert History Report")
            appendLine("Generated: ${SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())}")
            appendLine("Total alerts: ${items.size}")
            appendLine("==========================================")
            appendLine()
            items.forEachIndexed { index, item ->
                appendLine("Alert #${index + 1}")
                appendLine("  Date:     ${item.getDateDisplay()}")
                appendLine("  Time:     ${item.getTimeDisplay()}")
                appendLine("  Trigger:  ${item.getTriggerType()}")
                appendLine("  Location: ${item.getLocation()}")
                appendLine("  Status:   ${item.getStatus()}")
                appendLine("  Contacts: ${item.getContactsSent()} alerted")
                appendLine()
            }
        }
        val exportsDir = File(filesDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "pulsecheck_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt")
        try {
            file.writeText(report)
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Report saved internally", Toast.LENGTH_SHORT).show()
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PulseCheck SOS Alert History")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export history via..."))
        } catch (_: Exception) {
            Toast.makeText(this, "Saved to internal storage. Share not available.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::historyDb.isInitialized) historyDb.close()
    }
}
