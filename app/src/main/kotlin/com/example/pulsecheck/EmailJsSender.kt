package com.example.pulsecheck

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object EmailJsSender {
    private const val TAG = "EmailJsSender"
    private const val ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send"
    private const val SERVICE_ID = "service_k9j2uxq"
    private const val TEMPLATE_ID = "template_ud49y2i"
    private const val PUBLIC_KEY = "swXF5H0vgYyrGp8hC"

    interface EmailCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }

    @JvmStatic
    fun sendVerificationCode(toEmail: String, code: String, callback: EmailCallback?) {
        send(mapOf("email" to toEmail, "passcode" to code, "time" to "10 minutes"), callback)
    }

    @JvmStatic
    fun send(templateParams: Map<String, String>?, callback: EmailCallback?) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val body = JSONObject().apply {
                    put("service_id", SERVICE_ID)
                    put("template_id", TEMPLATE_ID)
                    put("user_id", PUBLIC_KEY)
                    put("template_params", JSONObject().apply {
                        templateParams?.forEach { (key, value) -> put(key, value) }
                    })
                }
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("origin", "https://pulsecheck.app")
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
                val status = connection.responseCode
                if (status in 200..299) postSuccess(callback)
                else postFailure(callback, "EmailJS error $status: ${readError(connection)}")
            } catch (e: Exception) {
                Log.e(TAG, "EmailJS request failed: ${e.message}", e)
                postFailure(callback, e.message ?: "Unknown network error")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun readError(connection: HttpURLConnection): String = try {
        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "(no error body)"
    } catch (_: Exception) {
        "(could not read error body)"
    }

    private fun postSuccess(callback: EmailCallback?) {
        if (callback != null) Handler(Looper.getMainLooper()).post { callback.onSuccess() }
    }

    private fun postFailure(callback: EmailCallback?, error: String) {
        if (callback != null) Handler(Looper.getMainLooper()).post { callback.onFailure(error) }
    }
}
