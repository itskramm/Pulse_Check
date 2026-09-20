package com.example.pulsecheck

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object NavHelper {
    const val TAB_HOME = 0
    const val TAB_LOCATION = 1
    const val TAB_CONTACTS = 2
    const val TAB_HISTORY = 3
    private const val COLOR_ACTIVE = 0xFFdd3c00.toInt()
    private const val COLOR_INACTIVE = 0xFF0e6995.toInt()

    @JvmStatic
    fun setup(activity: Activity, activeTab: Int) {
        val navIds = intArrayOf(R.id.nav_home, R.id.nav_location, R.id.nav_contacts, R.id.nav_history)
        val destinations = arrayOf(home::class.java, Location::class.java, Contacts::class.java, History::class.java)
        navIds.forEachIndexed { index, id ->
            val tab = activity.findViewById<LinearLayout>(id) ?: return@forEachIndexed
            highlightTab(tab, index == activeTab)
            if (index == activeTab) {
                tab.setOnClickListener(null)
            } else {
                tab.setOnClickListener {
                    val intent = Intent(activity, destinations[index]).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    activity.startActivity(intent)
                    if (index > activeTab) activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    else activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }
    }

    private fun highlightTab(tab: LinearLayout, active: Boolean) {
        val color = if (active) COLOR_ACTIVE else COLOR_INACTIVE
        for (i in 0 until tab.childCount) {
            when (val child = tab.getChildAt(i)) {
                is ImageView -> child.setColorFilter(color)
                is TextView -> {
                    child.setTextColor(color)
                    child.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                }
            }
        }
        tab.background = null
    }
}
