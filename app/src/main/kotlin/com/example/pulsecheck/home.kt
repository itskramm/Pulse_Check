package com.example.pulsecheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.telephony.SmsManager
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class home : AppCompatActivity() {

    private lateinit var settingsCard: CardView
    private lateinit var notificationsCard: CardView
    private lateinit var btnSettings: ImageView
    private lateinit var btnNotifications: ImageView
    private lateinit var btnDeadMansSwitch: ImageView
    private lateinit var tvSosStatus: TextView
    private lateinit var switchDarkMode: SwitchCompat

    private lateinit var session: SessionManager
    private lateinit var dbHelper: ContactDatabaseHelper
    private lateinit var historyDb: AlertHistoryDatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var countdownTimer: CountDownTimer? = null
    private var countdownDialog: AlertDialog? = null
    private var switchArmed = false
    private var isCountingDown = false
    private var pulseAnimation: PulseAnimationHelper? = null
    private var videoRecorder: VideoRecorderHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        session = SessionManager(this)
        dbHelper = ContactDatabaseHelper(this)
        historyDb = AlertHistoryDatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupMenus()
        setupDarkMode()
        setupNavigation()
        setupDeadMansSwitch()
        updateProfile()
        updateSosContactCount()
        updateContactsPreview()
        requestPermissionsIfNeeded()
        startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            updateProfile()
            updateSosContactCount()
            updateContactsPreview()
            if (!isCountingDown && !switchArmed) {
                pulseAnimation?.startIdlePulse()
            }
        }
    }

    private fun initViews() {
        btnSettings = findViewById(R.id.btnSettings)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnDeadMansSwitch = findViewById(R.id.btnDeadMansSwitch)
        settingsCard = findViewById(R.id.settings_menu_card)
        notificationsCard = findViewById(R.id.notifications_menu_card)
        tvSosStatus = findViewById(R.id.tvSosStatus)
        switchDarkMode = findViewById(R.id.switch_dark_mode)

        settingsCard.visibility = View.GONE
        notificationsCard.visibility = View.GONE
        pulseAnimation = PulseAnimationHelper(btnDeadMansSwitch)
        pulseAnimation?.startIdlePulse()
    }

    private fun setupMenus() {
        btnSettings.setOnClickListener {
            notificationsCard.visibility = View.GONE
            toggleCard(settingsCard)
        }
        btnNotifications.setOnClickListener {
            settingsCard.visibility = View.GONE
            toggleCard(notificationsCard)
        }

        settingsCard.findViewById<View>(R.id.btn_about_profile)?.setOnClickListener {
            closeMenus()
            startActivity(Intent(this, About_profile::class.java))
        }
        settingsCard.findViewById<View>(R.id.btn_absence_duration)?.setOnClickListener {
            closeMenus()
            showAbsenceDurationDialog()
        }
        settingsCard.findViewById<View>(R.id.btn_email_setup)?.setOnClickListener {
            closeMenus()
            showEmailSetupDialog()
        }
        settingsCard.findViewById<View>(R.id.btn_video_evidence)?.setOnClickListener {
            closeMenus()
            startActivity(Intent(this, VideoGalleryActivity::class.java))
        }
        settingsCard.findViewById<View>(R.id.btn_logout)?.setOnClickListener {
            closeMenus()
            confirmLogout()
        }
        notificationsCard.findViewById<View>(R.id.btn_see_all_notifications)?.setOnClickListener {
            closeMenus()
            startActivity(Intent(this, Notification::class.java))
        }

        findViewById<View>(R.id.contactsSearchBar).setOnClickListener {
            closeMenus()
            startActivity(Intent(this, Contacts::class.java))
        }
        findViewById<View>(R.id.profileSection).setOnClickListener {
            closeMenus()
            startActivity(Intent(this, About_profile::class.java))
        }
    }

    private fun setupDarkMode() {
        switchDarkMode.isChecked = session.isDarkMode()
        switchDarkMode.setOnCheckedChangeListener { _, checked ->
            session.setDarkMode(checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupNavigation() {
        NavHelper.setup(this, NavHelper.TAB_HOME)
    }

    private fun setupDeadMansSwitch() {
        btnDeadMansSwitch.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    switchArmed = true
                    cancelCountdown()
                    pulseAnimation?.onPressed()
                    startContinuousVideoRecording()
                    btnDeadMansSwitch.alpha = 0.6f
                }
                MotionEvent.ACTION_UP -> {
                    if (switchArmed) {
                        switchArmed = false
                        stopContinuousVideoRecording()
                        pulseAnimation?.onReleased()
                        btnDeadMansSwitch.alpha = 1f
                        if (!isCountingDown) startDeadMansCountdown()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    switchArmed = false
                    stopContinuousVideoRecording()
                    pulseAnimation?.onReleased()
                    btnDeadMansSwitch.alpha = 1f
                }
            }
            true
        }
    }

    private fun startDeadMansCountdown() {
        val seconds = session.getCountdownSeconds()
        isCountingDown = true
        pulseAnimation?.startAlertPulse()
        countdownDialog = AlertDialog.Builder(this)
            .setTitle("SOS Countdown")
            .setMessage("Sending SOS in $seconds seconds...")
            .setPositiveButton("Cancel SOS") { _, _ -> cancelCountdown() }
            .setCancelable(false)
            .show()

        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished + 999L) / 1000L
                tvSosStatus.text = "SOS in ${remaining}s - Tap Cancel SOS to stop!"
                tvSosStatus.setTextColor(getColor(R.color.pulse_alert))
                countdownDialog?.setMessage("Sending SOS in $remaining seconds...")
            }

            override fun onFinish() {
                isCountingDown = false
                countdownDialog?.dismiss()
                countdownDialog = null
                triggerSOS("Dead Man's Switch released")
            }
        }.start()
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        countdownDialog?.dismiss()
        countdownDialog = null
        isCountingDown = false
        session.setSosActive(false)
        pulseAnimation?.startIdlePulse()
        if (::tvSosStatus.isInitialized) {
            tvSosStatus.setTextColor(getColor(R.color.text_primary))
            updateSosContactCount()
        }
    }

    private fun triggerSOS(reason: String) {
        session.setSosActive(true)
        tvSosStatus.text = "SOS SENT!"
        tvSosStatus.setTextColor(getColor(R.color.pulse_alert))

        val phones = dbHelper.getAllPhoneNumbers()
        if (phones.isEmpty()) {
            historyDb.logAlert(reason, 0, "No Contacts", locationText(lastKnownLocation))
            Toast.makeText(this, "No emergency contacts set.", Toast.LENGTH_LONG).show()
            session.setSosActive(false)
            updateSosContactCount()
            return
        }

        val message = buildSosMessage(reason, lastKnownLocation)
        Thread {
            var successCount = 0
            phones.forEach { phone ->
                try {
                    sendSms(formatPhoneNumber(phone), message)
                    successCount++
                } catch (error: Exception) {
                    android.util.Log.e("PulseCheck", "Unable to send SOS SMS", error)
                }
            }
            historyDb.logAlert(
                reason,
                successCount,
                if (successCount == phones.size) "Sent" else "Partial",
                locationText(lastKnownLocation)
            )
            runOnUiThread {
                Toast.makeText(
                    this,
                    "SOS sent to $successCount of ${phones.size} contacts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun buildSosMessage(reason: String, location: Location?): String {
        val time = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(Date())
        val locationValue = locationText(location)
        val mapsLink = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "GPS unavailable"
        return "EMERGENCY ALERT\n${session.getUserName()} needs help.\n" +
            "Location: $locationValue\nMap: $mapsLink\nTime: $time\nReason: $reason"
    }

    private fun sendSms(phone: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("SMS permission not granted")
        }
        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
    }

    private fun formatPhoneNumber(phone: String): String {
        val normalized = phone.trim().replace(" ", "").replace("-", "")
        return if (normalized.startsWith("09")) "+63${normalized.substring(1)}" else normalized
    }

    private fun locationText(location: Location?): String =
        location?.let { "${it.latitude},${it.longitude}" } ?: "Unknown"

    private fun updateProfile() {
        findViewById<TextView>(R.id.tvProfileName).text = session.getUserName()
        findViewById<ImageView>(R.id.profile).setImageResource(session.getUserAvatar())
    }

    private fun updateSosContactCount() {
        if (::tvSosStatus.isInitialized) {
            val count = dbHelper.getContactCount()
            val label = if (count == 1) "person" else "people"
            tvSosStatus.text = "Your SOS will be sent to $count $label"
        }
    }

    private fun updateContactsPreview() {
        val placeholder = findViewById<TextView>(R.id.tvContactsPlaceholder)
        val scroll = findViewById<View>(R.id.contactsPreviewScroll)
        val list = findViewById<LinearLayout>(R.id.contactsPreviewList)
        val contacts = dbHelper.getAllContacts()
        if (contacts.isEmpty()) {
            placeholder.visibility = View.VISIBLE
            scroll.visibility = View.GONE
            return
        }

        placeholder.visibility = View.GONE
        scroll.visibility = View.VISIBLE
        list.removeAllViews()
        contacts.forEach { contact ->
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(12, 0, 12, 0)
            }
            val avatar = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(52, 52)
                setImageResource(contact.getImageResource())
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val name = TextView(this).apply {
                text = contact.getName().substringBefore(" ")
                textSize = 11f
                maxLines = 1
            }
            chip.addView(avatar)
            chip.addView(name)
            chip.setOnClickListener {
                startActivity(Intent(this, Contacts::class.java))
            }
            list.addView(chip)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(
                    this@home,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.SEND_SMS)
            if (ContextCompat.checkSelfPermission(
                    this@home,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (ContextCompat.checkSelfPermission(
                    this@home,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.CAMERA)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10_000L
            fastestInterval = 5_000L
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lastKnownLocation = result.lastLocation
            }
        }
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun showAbsenceDurationDialog() {
        val options = arrayOf("3 seconds", "5 seconds", "10 seconds")
        val values = intArrayOf(3, 5, 10)
        AlertDialog.Builder(this)
            .setTitle("SOS countdown duration")
            .setItems(options) { _, which -> session.setCountdownSeconds(values[which]) }
            .show()
    }

    private fun showEmailSetupDialog() {
        Toast.makeText(this, "Email SOS setup is available in settings.", Toast.LENGTH_SHORT).show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ ->
                session.clearSession()
                session.setLoggedIn(false)
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            .show()
    }

    private fun toggleCard(card: CardView) {
        card.visibility = if (card.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun closeMenus() {
        settingsCard.visibility = View.GONE
        notificationsCard.visibility = View.GONE
    }

    private fun startContinuousVideoRecording() {
        if (!ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            .equals(PackageManager.PERMISSION_GRANTED)
        ) return
        if (videoRecorder != null) return

        videoRecorder = VideoRecorderHelper(this).also { recorder ->
            recorder.startRecording(object : VideoRecorderHelper.RecordingCallback {
                override fun onRecordingStarted() = Unit
                override fun onSegmentComplete(file: java.io.File, segmentNumber: Int) = Unit
                override fun onRecordingFailed(error: String) {
                    android.util.Log.w("PulseCheck", "Video recording failed: $error")
                }
            })
        }
    }

    private fun stopContinuousVideoRecording() {
        videoRecorder?.stopRecording()
        videoRecorder = null
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        countdownDialog?.dismiss()
        pulseAnimation?.stopAllAnimations()
        stopContinuousVideoRecording()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        if (::dbHelper.isInitialized) dbHelper.close()
        if (::historyDb.isInitialized) historyDb.close()
        super.onDestroy()
    }
}
