package com.example.pulsecheck

import java.util.Locale
import kotlin.math.roundToLong

class AlertHistoryItem(
    private val triggerType: String,
    private val dateDisplay: String,
    private val timeDisplay: String,
    private val contactsSent: Int,
    private val status: String,
    private val location: String,
    private val pathDistance: Double = 0.0,
    private val pathDuration: Long = 0L,
    private val pathPoints: Int = 0
) {
    fun getTriggerType() = triggerType
    fun getDateDisplay() = dateDisplay
    fun getTimeDisplay() = timeDisplay
    fun getContactsSent() = contactsSent
    fun getStatus() = status
    fun getLocation() = location
    fun getPathDistance() = pathDistance
    fun getPathDuration() = pathDuration
    fun getPathPoints() = pathPoints

    fun getFormattedDistance(): String =
        if (pathDistance < 1000) "${pathDistance.roundToLong()} m"
        else String.format(Locale.US, "%.1f km", pathDistance / 1000)

    fun getFormattedDuration(): String {
        if (pathDuration == 0L) return "N/A"
        val seconds = pathDuration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) "$minutes min $remainingSeconds sec" else "$seconds sec"
    }

    fun hasPathData() = pathPoints > 0
}
