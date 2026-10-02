package com.example.pulsecheck

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils

/**
 * Helper class to manage pulse/heartbeat animations for the Dead Man's Switch button
 * 
 * Animation States:
 * - IDLE: Gentle pulsing heartbeat (like a resting heart rate ~60 BPM)
 * - PRESSED: Button pressed down, slightly compressed
 * - ALERT: Rapid pulsing during countdown (like elevated heart rate ~120 BPM)
 * 
 * Usage:
 * val pulseHelper = PulseAnimationHelper(buttonView)
 * pulseHelper.startIdlePulse()
 * pulseHelper.onPressed()
 * pulseHelper.onReleased()
 * pulseHelper.startAlertPulse()
 * pulseHelper.stopAllAnimations()
 */
class PulseAnimationHelper(private val view: View) {
    
    private var idleAnimator: AnimatorSet? = null
    private var alertAnimator: AnimatorSet? = null
    
    /**
     * Start gentle resting heartbeat animation (60 BPM)
     * Two beats with slight pause, like: lub-dub ... lub-dub
     */
    fun startIdlePulse() {
        stopAllAnimations()
        
        // Create heartbeat pattern: beat1 (150ms) + pause (100ms) + beat2 (120ms) + long pause (750ms)
        // Total cycle: ~1120ms ≈ 54 BPM (resting heart rate)
        
        val scaleX1 = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.12f, 1.0f)
        val scaleY1 = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.12f, 1.0f)
        scaleX1.duration = 150
        scaleY1.duration = 150
        
        val pause1 = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.0f)
        pause1.duration = 100
        
        val scaleX2 = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.06f, 1.0f)
        val scaleY2 = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.06f, 1.0f)
        scaleX2.duration = 120
        scaleY2.duration = 120
        
        val pause2 = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.0f)
        pause2.duration = 750
        
        idleAnimator = AnimatorSet().apply {
            playSequentially(
                AnimatorSet().apply { playTogether(scaleX1, scaleY1) },
                pause1,
                AnimatorSet().apply { playTogether(scaleX2, scaleY2) },
                pause2
            )
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Restart the animation for continuous pulse
                    if (idleAnimator != null) {
                        animation.start()
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }
    
    /**
     * Start rapid alert pulsing (120 BPM - elevated heart rate)
     * Continuous fast beats during countdown
     */
    fun startAlertPulse() {
        stopAllAnimations()
        
        // Fast heartbeat: 500ms cycle = 120 BPM (stressed/alert heart rate)
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.25f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.25f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.6f, 1.0f)
        
        scaleX.duration = 500
        scaleY.duration = 500
        alpha.duration = 500
        
        alertAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (alertAnimator === animation) {
                        animation.start()
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }
    
    /**
     * Button pressed - compress slightly with quick animation
     */
    fun onPressed() {
        // Don't stop idle pulse, just add press effect on top
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, view.scaleX, 0.92f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, view.scaleY, 0.92f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0.75f)
        
        scaleX.duration = 150
        scaleY.duration = 150
        alpha.duration = 150
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Button released - spring back with bounce
     */
    fun onReleased() {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, view.scaleX, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, view.scaleY, 1.0f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 1.0f)
        
        scaleX.duration = 300
        scaleY.duration = 300
        alpha.duration = 300
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = android.view.animation.OvershootInterpolator(1.5f)
            start()
        }
    }
    
    /**
     * Stop all animations and reset to normal state
     */
    fun stopAllAnimations() {
        idleAnimator?.cancel()
        idleAnimator = null
        
        alertAnimator?.cancel()
        alertAnimator = null
        
        // Reset to normal state
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.alpha = 1.0f
    }
    
    /**
     * Pause animations (keeps current state)
     */
    fun pause() {
        idleAnimator?.pause()
        alertAnimator?.pause()
    }
    
    /**
     * Resume animations from paused state
     */
    fun resume() {
        idleAnimator?.resume()
        alertAnimator?.resume()
    }
    
    /**
     * Check if any animation is running
     */
    fun isAnimating(): Boolean {
        return (idleAnimator?.isRunning == true) || (alertAnimator?.isRunning == true)
    }
}
