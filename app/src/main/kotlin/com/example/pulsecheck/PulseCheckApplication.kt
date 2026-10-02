package com.example.pulsecheck

import android.app.Application
import android.util.Log

/**
 * PulseCheck Application Class
 * Initializes global components and services
 */
class PulseCheckApplication : Application() {
    
    companion object {
        private const val TAG = "PulseCheckApp"
        lateinit var instance: PulseCheckApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "PulseCheck Application starting...")
        
        // Initialize Supabase
        initializeSupabase()
        
        // Initialize other components
        initializeSessionManager()
        
        Log.d(TAG, "PulseCheck Application initialized successfully")
    }
    
    /**
     * Initialize Supabase client
     */
    private fun initializeSupabase() {
        try {
            SupabaseClient.initialize(this)
            Log.d(TAG, "✅ Supabase initialized successfully")
            
            // Verify configuration
            if (SupabaseConfig.SUPABASE_URL.contains("your-project")) {
                Log.w(TAG, "⚠️ WARNING: Supabase credentials not configured!")
                Log.w(TAG, "⚠️ Please update SupabaseConfig.kt with your actual credentials")
            } else {
                Log.d(TAG, "✅ Supabase configured: ${SupabaseConfig.SUPABASE_URL}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Supabase", e)
            Log.e(TAG, "The app will continue but cloud features will not work")
        }
    }
    
    /**
     * Initialize Session Manager
     */
    private fun initializeSessionManager() {
        try {
            // SessionManager is already initialized on first access
            // But we can pre-load it here for faster first access
            val hasSession = SessionManager.isLoggedIn(this)
            Log.d(TAG, "Session status: ${if (hasSession) "Active" else "No session"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SessionManager", e)
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠️ Low memory warning - clearing caches")
        // Add any cache clearing logic here if needed
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "⚠️ Memory critically low while app running")
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "UI hidden - app in background")
            }
        }
    }
}
