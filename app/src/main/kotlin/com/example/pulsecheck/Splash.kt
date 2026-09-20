package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                session.isLoggedIn() -> Intent(this, home::class.java)
                session.isOnboardingDone() -> Intent(this, Welcome::class.java)
                else -> Intent(this, onboarding::class.java)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }, SPLASH_DURATION_MS)
    }

    companion object {
        private const val SPLASH_DURATION_MS = 2000L
    }
}
