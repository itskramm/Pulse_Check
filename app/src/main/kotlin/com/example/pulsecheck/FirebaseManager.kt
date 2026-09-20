package com.example.pulsecheck

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseManager private constructor() {
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var realtimeDb: DatabaseReference? = null
    private var lastLocationUpdate = 0L
    private var currentAlertId: String? = null
    private var alertStartTime = 0L

    init {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            realtimeDb = FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}")
        }
    }

    fun isAuthenticated() = auth?.currentUser != null
    fun getUserId(): String? = auth?.currentUser?.uid

    fun signInAnonymously(callback: AuthCallback) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            callback.onFailure("Firebase not initialized")
            return
        }
        if (isAuthenticated()) {
            callback.onSuccess(getUserId())
            return
        }
        firebaseAuth.signInAnonymously()
            .addOnSuccessListener { callback.onSuccess(it.user?.uid) }
            .addOnFailureListener { callback.onFailure(it.message ?: "Authentication failed") }
    }

    fun createSOSAlert(reason: String, latitude: Double, longitude: Double, contactCount: Int, callback: AlertCallback) {
        if (!isAuthenticated()) {
            callback.onFailure("User not authenticated")
            return
        }
        if (currentAlertId != null && !isAlertExpired()) {
            callback.onSuccess(currentAlertId)
            return
        }
        val alert = hashMapOf<String, Any?>(
            "userId" to getUserId(), "reason" to reason, "startTime" to FieldValue.serverTimestamp(),
            "latitude" to latitude, "longitude" to longitude, "contactCount" to contactCount,
            "status" to "active", "locationUpdates" to 0
        )
        firestore?.collection("active_alerts")?.add(alert)
            ?.addOnSuccessListener {
                currentAlertId = it.id
                alertStartTime = System.currentTimeMillis()
                callback.onSuccess(currentAlertId)
            }?.addOnFailureListener { callback.onFailure(it.message ?: "Failed to create alert") }
    }

    fun updateLocation(latitude: Double, longitude: Double, accuracy: Float, speed: Float) {
        if (!isAuthenticated() || currentAlertId == null || isAlertExpired()) {
            if (isAlertExpired() && currentAlertId != null) stopAlert()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastLocationUpdate < MIN_UPDATE_INTERVAL) return
        lastLocationUpdate = now
        val locationData = mapOf(
            "lat" to latitude, "lng" to longitude, "accuracy" to accuracy, "speed" to speed,
            "timestamp" to now, "alertId" to currentAlertId
        )
        val userId = getUserId() ?: return
        realtimeDb?.child("live_locations")?.child(userId)?.setValue(locationData)
            ?.addOnSuccessListener { incrementUpdateCounter() }
    }

    private fun incrementUpdateCounter() {
        val alertId = currentAlertId ?: return
        firestore?.collection("active_alerts")?.document(alertId)
            ?.update("locationUpdates", FieldValue.increment(1))
    }

    private fun isAlertExpired() =
        alertStartTime == 0L || System.currentTimeMillis() - alertStartTime > MAX_ALERT_DURATION

    fun stopAlert() {
        val alertId = currentAlertId ?: return
        firestore?.collection("active_alerts")?.document(alertId)
            ?.update("status", "resolved", "endTime", FieldValue.serverTimestamp())
            ?.addOnSuccessListener {
                moveAlertToHistory(alertId)
                currentAlertId = null
                alertStartTime = 0
            }
        getUserId()?.let { realtimeDb?.child("live_locations")?.child(it)?.removeValue() }
    }

    private fun moveAlertToHistory(alertId: String) {
        firestore?.collection("active_alerts")?.document(alertId)?.get()?.addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                firestore?.collection("alert_history")?.document(alertId)?.set(snapshot.data ?: emptyMap<String, Any>())
                    ?.addOnSuccessListener { firestore?.collection("active_alerts")?.document(alertId)?.delete() }
            }
        }
    }

    fun getCurrentAlertId() = currentAlertId
    fun isAlertActive() = currentAlertId != null && !isAlertExpired()

    fun getUsageStats(callback: UsageCallback) {
        val alertId = currentAlertId
        if (alertId == null) {
            callback.onStats(0, 0)
            return
        }
        firestore?.collection("active_alerts")?.document(alertId)?.get()?.addOnSuccessListener { snapshot ->
            val updates = snapshot.getLong("locationUpdates")?.toInt() ?: 0
            callback.onStats(updates, (System.currentTimeMillis() - alertStartTime) / 1000)
        }
    }

    interface AuthCallback {
        fun onSuccess(userId: String?)
        fun onFailure(error: String)
    }

    interface AlertCallback {
        fun onSuccess(alertId: String?)
        fun onFailure(error: String)
    }

    fun interface UsageCallback {
        fun onStats(locationUpdates: Int, durationSeconds: Long)
    }

    companion object {
        private const val TAG = "FirebaseManager"
        private const val MIN_UPDATE_INTERVAL = 60_000L
        private const val MAX_ALERT_DURATION = 30 * 60 * 1000L
        @JvmStatic
        @Synchronized
        fun getInstance(): FirebaseManager = INSTANCE
        private val INSTANCE = FirebaseManager()
    }
}
