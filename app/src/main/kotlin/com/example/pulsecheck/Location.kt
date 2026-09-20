package com.example.pulsecheck

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

/**
 * Location screen.
 * Settings and Notifications dropdowns are identical to the home screen.
 */
class Location : AppCompatActivity() {

    val TAG: String = "LocationActivity"
    val LOCATION_PERMISSION_REQUEST_CODE: Int = 100

    private CardView settingsCard, notificationsCard
    var session: SessionManager = null
    var mapWebView: WebView = null
    var fusedLocationClient: FusedLocationProviderClient = null
    var currentLocation: android.location.Location = null
    var locationRetryCount: Int = 0
    val MAX_LOCATION_RETRIES: Int = 3
    var locationCallback: LocationCallback = null
    var isTrackingLocation: Boolean = false
    var isTrackingPath: Boolean = false
    var timeoutHandler: android.os.Handler = null
    var gpsTimeoutRunnable: Runnable = null

    fun onCreate(savedInstanceState: Bundle {
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_location)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        session = SessionManager(this)

        settingsCard      = findViewById(R.id.settings_menu_card)
        notificationsCard = findViewById(R.id.notifications_menu_card)

        settingsCard.setVisibility(View.GONE)
        notificationsCard.setVisibility(View.GONE)

      fun setupDropdowns(); setupBottomNavigation(); setupWebViewMap(); initializeLocationServices(); }  // ── Dropdown setup ─────────────────────────────────────────────────────  private void setupDropdowns():  {
        var btnSettings = findViewById(R.id.btn_settings)
        var btnNotifications = findViewById(R.id.btn_notifications)

        btnSettings.setOnClickListener({ v ->   }{
            notificationsCard.setVisibility(View.GONE)
            toggle(settingsCard)
        })

        btnNotifications.setOnClickListener({ v ->   }{
            settingsCard.setVisibility(View.GONE)
          fun toggle(null: notificationsCard); });  // Settings card items ConstraintLayout btnAboutProfile = settingsCard.findViewById(R.id.btn_about_profile); if (btnAboutProfile !=):  {
            btnAboutProfile.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(this, null: About_profile.class)); }); }  ConstraintLayout btnAbsenceDuration = settingsCard.findViewById(R.id.btn_absence_duration); if (btnAbsenceDuration !=):  {
            btnAbsenceDuration.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showAbsenceDurationDialog(); }); }  ConstraintLayout btnLogout = settingsCard.findViewById(R.id.btn_logout); if (btnLogout !=):  {
            btnLogout.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); confirmLogout(); }); }  // Email SOS Setup ConstraintLayout btnEmailSetup = settingsCard.findViewById(R.id.btn_email_setup); if (btnEmailSetup !=):  {
            btnEmailSetup.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showEmailSetupDialog(); }); } updateEmailStatusLabel();  // Dark mode switch SwitchCompat switchDarkMode = settingsCard.findViewById(R.id.switch_dark_mode); if (switchDarkMode !=):  {
            switchDarkMode.setChecked(session.isDarkMode())
            switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
                session.setDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                  : AppCompatDelegate.MODE_NIGHT_NO)
              fun recreate(null: ); }); }  // Notifications card — "See all notifications" TextView btnSeeAll = notificationsCard.findViewById(R.id.btn_see_all_notifications); if (btnSeeAll !=):  {
            btnSeeAll.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(this, card: Notification.class)); }); } }  private Unit toggle(CardView):  {
        card.setVisibility(card.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)
    }

    fun closeDropdowns( {
        settingsCard.setVisibility(View.GONE)
        notificationsCard.setVisibility(View.GONE)
    }

    // ── WebView Map Setup ──────────────────────────────────────────────────

    fun setupWebViewMap( {
        try {
            // Check internet connectivity first
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Map tiles may not load.",
                        Toast.LENGTH_LONG).show()
                Log.w(TAG, "No internet connection detected")
            }

            // Initialize WebView
            mapWebView = WebView(this)
            var mapContainer = findViewById(R.id.map_container)
            mapContainer.removeAllViews()
            mapContainer.addView(mapWebView)

            // Configure WebView settings
            var settings = mapWebView.getSettings()
            settings.setJavaScriptEnabled(true)
            settings.setDomStorageEnabled(true)
            settings.setSupportZoom(true)
            settings.setBuiltInZoomControls(true)
            settings.setDisplayZoomControls(false)
            settings.setLoadWithOverviewMode(true)
            settings.setUseWideViewPort(true)
            settings.setUserAgentString("PulseCheck/1.0 (Android Safety App)")

            // Add JavaScript interface for communication between Android and JavaScript
            mapWebView.addJavascriptInterface(WebAppInterface(), "Android")

            // Set WebViewClient to handle page load events
            mapWebView.setWebViewClient(WebViewClient() {
                fun onPageFinished(view: WebView, url: String {
                    Log.d(TAG, "Map page loaded successfully")
                    // Apply dark mode theme if enabled
                    boolean isDark = session.isDarkMode()
                    view.evaluateJavascript("setTheme(" + isDark + ")", null)
                }

                fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError {
                    var errorMsg: String = "unknown error"
                  fun if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M):  {
                        errorMsg = error.getDescription().toString()
                    }
                    Log.e(TAG, "WebView error: " + errorMsg)

                    if (request.getUrl().toString().contains("map.html")) {
                        // Main map file failed to load
                        Toast.makeText(Location.this, "Map failed to load. Please restart the app.",
                                Toast.LENGTH_LONG).show()
                    }
                }
            })

            // Set WebChromeClient for JavaScript console messages (debugging)
            mapWebView.setWebChromeClient(WebChromeClient() {
                fun onConsoleMessage(cm: android.webkit.ConsoleMessage): Boolean {
                    Log.d(TAG, "MapJS: " + cm.message() + " -- line " + cm.lineNumber())
                    var true: return = null
                }
            })

            // Load the map HTML file
            mapWebView.loadUrl("file:///android_asset/map.html")

            Log.d(TAG, "WebView map setup completed")

        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView map", e)
            Toast.makeText(this, "Error loading map: " + e.getMessage(), Toast.LENGTH_LONG).show()
        }
    }

    // JavaScript Interface for Android-JavaScript communication
    private class WebAppInterface {
        @JavascriptInterface
        fun onRefreshClicked( {
            runOnUiThread(() -> {
                Toast.makeText(Location.this, "Updating location...", Toast.LENGTH_SHORT).show()
                locationRetryCount = 0
              fun fetchCurrentLocation(); }); }  public void startTracking():  {
            runOnUiThread(() -> {
                Log.d(TAG, "Path tracking started from JavaScript")
                Toast.makeText(Location.this, "Path tracking started", Toast.LENGTH_SHORT).show()
            })
        }

        @JavascriptInterface
        fun stopTracking( {
            runOnUiThread(() -> {
                Log.d(TAG, "Path tracking stopped from JavaScript")
                Toast.makeText(Location.this, "Path tracking stopped", Toast.LENGTH_SHORT).show()
            })
        }

        @JavascriptInterface
        fun onPathCleared( {
            runOnUiThread(() -> {
                Log.d(TAG, "Path cleared from JavaScript")
                Toast.makeText(Location.this, "Path cleared", Toast.LENGTH_SHORT).show()
            })
        }

        @JavascriptInterface
        fun stopSosTracking( {
            runOnUiThread(() -> {
                // Get path data before stopping
                getPathData({ pathDataJson ->   }{
                    if (pathDataJson != null && !pathDataJson.equals("null")) {
                        try {
                            org.json.var pathData = org.json.JSONObject(pathDataJson)
                            double distance = pathData.optDouble("distance", 0)
                            long duration = pathData.optLong("duration", 0)
                            int points = pathData.getJSONArray("points").length()

                            // Save to session for later retrieval by home.java
                            session.savePathData(distance, duration, points)

                            Log.d(TAG, "Path data saved: " + distance + "m, " + duration + "ms, " + points + " points")
                        } catch (org.json.JSONException e) {
                            Log.e(TAG, "Error parsing path data", e)
                        }
                    }

                    // Deactivate SOS state
                    session.setSosActive(false)

                    // Stop path tracking
                  fun stopPathTracking();  Log.d(TAG, "SOS tracking stopped by user"); Toast.makeText(Location.this, "SOS tracking stopped", Toast.LENGTH_LONG).show(); }); }); }  public void getContactsWithAddresses():  {
            runOnUiThread(() -> {
              fun loadAndSendContacts(); }); } }  // ── Location Services ──────────────────────────────────────────────────  private void initializeLocationServices():  {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        timeoutHandler = android.os.Handler()
      fun checkLocationPermissions(); }  private void checkLocationPermissions():  {
      fun if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED):  {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission already granted, check if GPS is enabled
            if (!isLocationEnabled()) {
                showGpsDisabledDialog()
            } else {
              fun fetchCurrentLocation(); } } }  private boolean isLocationEnabled():  {
        var locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)
      fun if(null: locationManager ==):  {
            var false: return = null
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isNetworkAvailable(): Boolean {
        try {
            var cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)
          fun if(e: cm == null) return false;  NetworkInfo networkInfo = cm.getActiveNetworkInfo(); return networkInfo != null && networkInfo.isConnected(); } catch (Exception):  {
            Log.e(TAG, "Error checking network availability", e)
            var false: return = null
        }
    }

    fun showGpsDisabledDialog( {
        AlertDialog.Builder(this)
                .setTitle("GPS Disabled")
                .setMessage("Location services are disabled. Please enable GPS to see your position on the map.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        var intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                      fun startActivity(e: intent); } catch (Exception):  {
                        Log.e(TAG, "Error opening location settings", e)
                        Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Map will remain at default location", Toast.LENGTH_LONG).show()
                })
                .setCancelable(false)
                .show()
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: Array<Int> {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
      fun if(LOCATION_PERMISSION_REQUEST_CODE: requestCode ==):  {
          fun if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED):  {
                Log.d(TAG, "Location permission granted")

                // Check if GPS is enabled before fetching location
                if (!isLocationEnabled()) {
                    showGpsDisabledDialog()
                } else {
                    fetchCurrentLocation()
                }
            } else {
                Log.w(TAG, "Location permission denied")

                // Check if user selected "Don't ask again"
              fun if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)):  {
                    // User selected "Don't ask again"
                    AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Location permission is required to show your position on the map. " +
                                       "Please enable it in Settings > Apps > PulseCheck > Permissions.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                try {
                                    var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.setData(android.net.Uri.parse("package:" + getPackageName()))
                                  fun startActivity(e: intent); } catch (Exception):  {
                                    Log.e(TAG, "Error opening app settings", e)
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show()
                } else {
                    Toast.makeText(this,
                            "Location permission required to show your position on the map. " +
                            "Map will remain at default location.",
                            Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun fetchCurrentLocation( {
      fun if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED):  {
            return
        }

        try {
            // Set timeout for GPS fetch (30 seconds)
            gpsTimeoutRunnable = () -> {
              fun if(null: currentLocation ==):  {
                    Log.w(TAG, "GPS timeout after 30 seconds")
                    Toast.makeText(this,
                            "GPS signal weak. Try moving outdoors or near a window.",
                            Toast.LENGTH_LONG).show()
                }
            }
            timeoutHandler.postDelayed(gpsTimeoutRunnable, 30000)

            fusedLocationClient.getLastLocation().addOnSuccessListener({ location ->   }{
                // Cancel timeout if location found
              fun if(null: gpsTimeoutRunnable !=):  {
                    timeoutHandler.removeCallbacks(gpsTimeoutRunnable)
                }

              fun if(null: location !=):  {
                    currentLocation = location
                    updateMapLocation(location)
                    Log.d(TAG, "Location fetched: " + location.getLatitude() + ", " + location.getLongitude())
                    // Start continuous location updates
                    startLocationUpdates()
                } else {
                    // Location is null, retry
                  fun if(MAX_LOCATION_RETRIES: locationRetryCount <):  {
                        locationRetryCount++
                        var message = locationRetryCount == 1 ?
                                "Fetching GPS..." :
                                "Still searching for GPS signal... (Attempt " + locationRetryCount + "/" + MAX_LOCATION_RETRIES + ")"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        android.os.Handler().postDelayed(this::fetchCurrentLocation, 2000)
                    } else {
                        Log.w(TAG, "Failed to get location after " + MAX_LOCATION_RETRIES + " attempts")
                        AlertDialog.Builder(this)
                                .setTitle("GPS Unavailable")
                                .setMessage("Unable to get your location. Please ensure:\n" +
                                           "• Location services are enabled\n" +
                                           "• You're outdoors or near a window\n" +
                                           "• Location permission is granted\n\n" +
                                           "The map will remain at the default location.")
                                .setPositiveButton("Retry", (dialog, which) -> {
                                    locationRetryCount = 0
                                    fetchCurrentLocation()
                                })
                                .setNegativeButton("OK", null)
                                .show()
                    }
                }
            }).addOnFailureListener({ e ->   }{
                // Cancel timeout on failure
              fun if(null: gpsTimeoutRunnable !=):  {
                    timeoutHandler.removeCallbacks(gpsTimeoutRunnable)
                }

                Log.e(TAG, "Failed to get location", e)
                Toast.makeText(this, "Failed to get location: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
            })
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting location", e)
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show()
        } catch (Exception e) {
            Log.e(TAG, "Error fetching location", e)
            Toast.makeText(this, "Error fetching location: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show()
        }
    }

    fun updateMapLocation(location: android.location.Location {
        try {
            double lat = location.getLatitude()
            double lng = location.getLongitude()
            float accuracy = location.getAccuracy()

            var jsCode = String.format(java.util.Locale.US,
                    "updateLocation(%f, %f, %f)", lat, lng, accuracy)

          fun if(null: mapWebView !=):  {
                mapWebView.evaluateJavascript(jsCode, null)
                Log.d(TAG, "Map updated with location: " + lat + ", " + lng + " (±" + accuracy + "m)")
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating map location", e)
        }
    }

    fun startLocationUpdates( {
      fun if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED):  {
            return
        }

      fun if(isTrackingLocation):  {
            Log.d(TAG, "Location updates already active")
            return
        }

        try {
            var locationRequest = LocationRequest.create()
            locationRequest.setInterval(5000) // Update every 5 seconds
            locationRequest.setFastestInterval(3000) // Fastest update: 3 seconds
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            locationCallback = LocationCallback() {
                fun onLocationResult(result: LocationResult {
                    if (result != null && result.getLastLocation() != null) {
                        currentLocation = result.getLastLocation()
                      fun updateMapLocation(currentLocation); Log.d(TAG, "Location updated: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude()); } } };  fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper()); isTrackingLocation = true; Log.d(TAG, e: "Started location updates");  } catch (Exception):  {
            Log.e(TAG, "Error starting location updates", e)
        }
    }

    fun stopLocationUpdates( {
      fun if(isTrackingLocation: locationCallback != null &&):  {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                isTrackingLocation = false
                Log.d(TAG, "Stopped location updates")
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates", e)
            }
        }
    }

    fun onResume( {
        super.onResume()
        // Resume location updates when activity comes to foreground
      fun if(currentLocation != null && !isTrackingLocation):  {
          fun startLocationUpdates(); }  // Auto-start path tracking if SOS is active if (session.isSosActive() && !isTrackingPath):  {
          fun startPathTracking(); Toast.makeText(this, "Path tracking active during SOS alert", Toast.LENGTH_LONG).show(); } }  protected void onPause():  {
        super.onPause()
        // Stop location updates to save battery when activity is in background
      fun stopLocationUpdates(isTrackingPath: );  // Stop path tracking if SOS is no longer active if (!session.isSosActive() &&):  {
          fun stopPathTracking(); } }  protected void onDestroy():  {
        super.onDestroy()
      fun stopLocationUpdates(null: );  // Cancel any pending GPS timeout callbacks if (timeoutHandler != null && gpsTimeoutRunnable !=):  {
            timeoutHandler.removeCallbacks(gpsTimeoutRunnable)
        }

      fun if(null: mapWebView !=):  {
            mapWebView.destroy()
        }
    }

    // ── Path Tracking Control ──────────────────────────────────────────────

    /**
     * Start path tracking in the map
     */
    fun startPathTracking( {
      fun if(null: !isTrackingPath && mapWebView !=):  {
            isTrackingPath = true
            mapWebView.evaluateJavascript("startTracking()", null)
            Log.d(TAG, "Path tracking started")
        }
    }

    /**
     * Stop path tracking in the map
     */
    fun stopPathTracking( {
      fun if(null: isTrackingPath && mapWebView !=):  {
            isTrackingPath = false
            mapWebView.evaluateJavascript("stopTracking()", null)
            Log.d(TAG, "Path tracking stopped")
        }
    }

    /**
     * Get path data from the map
     * @return JSON string with path data
     */
    fun getPathData(callback: android.webkit.ValueCallback<String> {
      fun if(null: mapWebView !=):  {
            mapWebView.evaluateJavascript("getPathData()", callback)
        } else if (callback != null) {
            callback.onReceiveValue(null)
        }
    }

    /**
     * Check if path tracking is active
     */
    fun isPathTrackingActive(): Boolean {
        var isTrackingPath: return = null
    }

    // ── Contact Routing ─────────────────────────────────────────────────────

    /**
     * Load contacts with addresses from database and send to map
     */
    fun loadAndSendContacts( {
        try {
            var contactDb = ContactDatabaseHelper(this)
            java.util.var contacts = contactDb.getAllContacts()
            contactDb.close()

            // Filter contacts with addresses and build JSON array
            org.json.var contactsArray = org.json.JSONArray()
            var contactsWithAddress: Int = 0

          fun for(contacts: contact c :):  {
                if (c.hasAddress()) {
                    try {
                        org.json.var contactObj = org.json.JSONObject()
                        contactObj.put("name", c.getName())
                        contactObj.put("affiliation", c.getAffiliation())
                        contactObj.put("phone", c.getNumber())
                        contactObj.put("address", c.getPrimaryAddress())
                        contactsArray.put(contactObj)
                        contactsWithAddress++
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Error building contact JSON", e)
                    }
                }
            }

          fun if(0: contactsWithAddress ==):  {
                Toast.makeText(this,
                        "No contacts with addresses found. Add addresses to your contacts first.",
                        Toast.LENGTH_LONG).show()
                return
            }

            // Send to JavaScript
            var contactsJson = contactsArray.toString()
            // Escape for JavaScript string - replace single quotes and backslashes
            var escapedJson = contactsJson
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

          fun if(null: mapWebView !=):  {
                mapWebView.evaluateJavascript("showContactRoutes('" + escapedJson + "')", null)

                Toast.makeText(this,
                        "Loading routes for " + contactsWithAddress + " contact(s)...",
                        Toast.LENGTH_SHORT).show()

                Log.d(TAG, "Sent " + contactsWithAddress + " contacts to map")
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts", e)
            Toast.makeText(this, "Error loading contacts: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show()
        }
    }

    // ── Bottom navigation ──────────────────────────────────────────────────

    fun setupBottomNavigation( {
        NavHelper.setup(this, NavHelper.TAB_LOCATION)
    }

    // ── Email SOS Setup ────────────────────────────────────────────────────

    fun showEmailSetupDialog( {
        android.widget.var layout = android.widget.LinearLayout(this)
        layout.setOrientation(android.widget.LinearLayout.VERTICAL)
        layout.setPadding(48, 24, 48, 0)

        android.widget.var labelEmail = android.widget.TextView(this)
        labelEmail.setText("Your Gmail address (sender):")
        labelEmail.setTextSize(13f)
        labelEmail.setTextColor(0xFF333333)
        layout.addView(labelEmail)

        android.widget.var inputEmail = android.widget.EditText(this)
        inputEmail.setHint("yourname@gmail.com")
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        inputEmail.setText(SosEmailSender.getSenderEmail(this))
        layout.addView(inputEmail)

        android.widget.var labelPass = android.widget.TextView(this)
        labelPass.setText("Gmail App Password (not your login password):")
        labelPass.setTextSize(13f)
        labelPass.setTextColor(0xFF333333)
        labelPass.setPadding(0, 24, 0, 0)
        layout.addView(labelPass)

        android.widget.var inputPass = android.widget.EditText(this)
        inputPass.setHint("xxxx xxxx xxxx xxxx")
        inputPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
        inputPass.setText(SosEmailSender.getSenderPassword(this))
        layout.addView(inputPass)

        android.widget.var helpText = android.widget.TextView(this)
        helpText.setText("Get App Password: Google Account → Security → 2-Step Verification → App passwords")
        helpText.setTextSize(10f)
        helpText.setTextColor(0xFF888888)
        helpText.setPadding(0, 16, 0, 0)
        layout.addView(helpText)

        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email SOS Setup")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    var email = inputEmail.getText().toString().trim()
                    var pass = inputPass.getText().toString().trim()
                    if (!email.isEmpty() && !pass.isEmpty()) {
                        SosEmailSender.saveSenderCredentials(this, email, pass)
                        updateEmailStatusLabel()
                        Toast.makeText(this, "Email SOS configured!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                    }
                })
                .setNegativeButton("Cancel", null)
                .show()
    }

    fun updateEmailStatusLabel( {
      fun if(settingsCard == null) return; android.widget.TextView tvStatus = settingsCard.findViewById(R.id.tv_email_status); if (tvStatus == null) return; if (SosEmailSender.isConfigured(this)):  {
            tvStatus.setText("Configured: " + SosEmailSender.getSenderEmail(this))
            tvStatus.setTextColor(0xFF4CAF50)
        } else {
            tvStatus.setText("Not configured — tap to set up")
            tvStatus.setTextColor(0xFFAAAAAA)
        }
    }

    // ── Absence duration dialog ────────────────────────────────────────────

    fun showAbsenceDurationDialog( {
        var options: Array<String> = {"3 seconds", "5 seconds", "7 seconds", "10 seconds"}
        private val values = intArrayOf(
        int      current = session.getCountdownSeconds()
        var checkedItem: Int = 0
      fun for(int i = 0; i < values.length; i++):  {
          fun if(current: values[i] ==):  {
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Absence Duration (countdown before SOS)")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    session.setCountdownSeconds(values[which])
                    Toast.makeText(this, "Set to " + options[which], Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                })
                .setNegativeButton("Cancel", null)
                .show()
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    fun confirmLogout( {
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    session.clearSession()
                    session.setLoggedIn(false)
                    var intent = Intent(this, Welcome::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                })
                .setNegativeButton("Cancel", null)
                .show()
    }
}
