package com.example.pulsecheck

/**
 * Supabase Configuration
 * 
 * IMPORTANT: Replace these values with your actual Supabase credentials
 * 
 * To get your credentials:
 * 1. Go to https://supabase.com/dashboard
 * 2. Select your project
 * 3. Go to Settings → API
 * 4. Copy the Project URL and anon/public key
 * 
 * SECURITY NOTE:
 * - The anon key is safe to use in mobile apps
 * - Row Level Security (RLS) policies protect your data
 * - Never commit service_role keys to version control
 */
object SupabaseConfig {
    /**
     * Your Supabase Project URL
     * Format: https://your-project-id.supabase.co
     */
    const val SUPABASE_URL = "https://yfpmsaqxxfrqchkduhta.supabase.co"
    
    /**
     * Your Supabase Anon/Public Key
     * This key is safe to use in mobile apps
     */
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcG1zYXF4eGZycWNoa2R1aHRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4OTcxMTYsImV4cCI6MjEwNjQ3MzExNn0.81aCYKib-biNwMi9WsfWbrPtkbw_3-cqaQ4aJqx-H20"
    
    /**
     * Storage bucket name for encrypted videos
     */
    const val VIDEO_BUCKET_NAME = "sos-videos"
    
    /**
     * Maximum video file size (100MB)
     */
    const val MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024L
    
    /**
     * Enable Supabase debug logging
     * Set to false in production
     */
    const val DEBUG_MODE = BuildConfig.DEBUG
}
