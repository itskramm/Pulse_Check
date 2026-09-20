package com.example.pulsecheck

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Notification : AppCompatActivity() {
    private lateinit var notifDb: NotificationDatabaseHelper
    private var notifList: LinearLayout? = null
    private var tvEmpty: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        notifDb = NotificationDatabaseHelper(this)
        notifList = findViewById(R.id.notifList)
        tvEmpty = findViewById(R.id.tvNotifEmpty)
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnDeleteAll)?.setOnClickListener { confirmDeleteAll() }
        loadNotifications()
    }

    private fun loadNotifications() {
        val list = notifList ?: return
        list.removeAllViews()
        val items = notifDb.getAll()
        if (items.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            return
        }
        tvEmpty?.visibility = View.GONE
        val dp = resources.displayMetrics.density.toInt()
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16 * dp, 12 * dp, 16 * dp, 12 * dp)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            }
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(40 * dp, 40 * dp).apply { marginEnd = 16 * dp }
                if (item.type == NotificationDatabaseHelper.TYPE_SOS_SENT) {
                    setImageResource(R.drawable.heart)
                    setColorFilter(0xFFdd3c00.toInt())
                } else {
                    setImageResource(R.drawable.contacts)
                    setColorFilter(0xFF0e6995.toInt())
                }
            }
            row.addView(icon)
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = item.message
                setTextColor(Color.DKGRAY)
                textSize = 14f
            })
            textCol.addView(TextView(this).apply {
                text = item.time
                setTextColor(Color.GRAY)
                textSize = 11f
            })
            row.addView(textCol)
            list.addView(row)
            list.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(16 * dp, 0, 16 * dp, 0) }
                setBackgroundColor(0xFFD3D3E5.toInt())
            })
        }
    }

    private fun confirmDeleteAll() {
        if (notifDb.getCount() == 0) {
            Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Clear Notifications")
            .setMessage("Delete all notifications?")
            .setPositiveButton("Delete All") { _, _ ->
                notifDb.clearAll()
                loadNotifications()
                Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::notifDb.isInitialized) notifDb.close()
    }
}
