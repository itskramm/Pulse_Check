package com.example.pulsecheck

import android.app.Application

class PulseCheckApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.initialize(this)
    }
}
