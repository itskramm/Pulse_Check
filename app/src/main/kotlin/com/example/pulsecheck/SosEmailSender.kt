package com.example.pulsecheck

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object SosEmailSender {
    private const val TAG = "SosEmailSender"
    private const val PREFS_NAME = "PulseCheckEmailPrefs"
    private const val KEY_EMAIL = "sender_email"
    private const val KEY_PASSWORD = "sender_app_password"

    interface EmailCallback {
        fun onSuccess(sentCount: Int)
        fun onFailure(error: String)
    }

    @JvmStatic
    fun saveSenderCredentials(ctx: Context, email: String, appPassword: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_EMAIL, email.trim()).putString(KEY_PASSWORD, appPassword.trim()).apply()
    }

    @JvmStatic fun getSenderEmail(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""
    @JvmStatic fun getSenderPassword(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PASSWORD, "") ?: ""
    @JvmStatic fun isConfigured(ctx: Context) = getSenderEmail(ctx).isNotEmpty() && getSenderPassword(ctx).isNotEmpty()

    @JvmStatic
    fun sendSosEmails(
        ctx: Context, recipientEmails: List<String>?, senderName: String, locationText: String,
        mapsLink: String, timestamp: String, triggerReason: String, callback: EmailCallback?
    ) {
        val senderEmail = getSenderEmail(ctx)
        val senderPassword = getSenderPassword(ctx)
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            postFailure(callback, "Email not configured. Set up sender email in Settings.")
            return
        }
        if (recipientEmails.isNullOrEmpty()) {
            postFailure(callback, "No email addresses saved for contacts.")
            return
        }
        Thread {
            var successCount = 0
            var lastError: String? = null
            try {
                val session = createGmailSession(senderEmail, senderPassword)
                val subject = "SOS ALERT - $senderName needs help!"
                val body = buildEmailBody(senderName, locationText, mapsLink, timestamp, triggerReason)
                recipientEmails.forEach { recipient ->
                    try {
                        MimeMessage(session).apply {
                            setFrom(InternetAddress(senderEmail, senderName))
                            addRecipient(Message.RecipientType.TO, InternetAddress(recipient))
                            setSubject(subject)
                            setText(body)
                        }.also(Transport::send)
                        successCount++
                    } catch (e: Exception) {
                        lastError = e.message
                        Log.e(TAG, "Failed to send to $recipient", e)
                    }
                }
            } catch (e: Exception) {
                lastError = e.message
                Log.e(TAG, "SMTP session error", e)
            }
            Handler(Looper.getMainLooper()).post {
                if (callback != null) {
                    if (successCount > 0) callback.onSuccess(successCount)
                    else callback.onFailure(lastError ?: "Unknown error — check Logcat for SosEmailSender")
                }
            }
        }.start()
    }

    private fun createGmailSession(email: String, password: String): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.writetimeout", "15000")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(email, password)
        })
    }

    private fun buildEmailBody(name: String, locationText: String, mapsLink: String, timestamp: String, triggerReason: String) =
        "EMERGENCY SOS ALERT\n==========================================\n\n" +
            "$name needs immediate help!\n\nTIME:     $timestamp\nLOCATION: $locationText\n" +
            "MAP LINK: $mapsLink\nTRIGGER:  $triggerReason\n\n==========================================\n" +
            "Please respond immediately or contact emergency services (911).\n\n" +
            "This alert was sent automatically by PulseCheck Safety App."

    private fun postFailure(callback: EmailCallback?, error: String) {
        if (callback != null) Handler(Looper.getMainLooper()).post { callback.onFailure(error) }
    }
}
