package com.example.pulsecheck

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure video storage manager
 * Videos are encrypted and stored in database-backed secure storage
 * NOT directly accessible by users or other apps
 */
class SecureVideoStorage(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureVideoStorage"
        private const val KEY_ALIAS = "pulsecheck_video_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }
    
    init {
        // Generate encryption key if not exists
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }
    
    /**
     * Generate AES encryption key in Android KeyStore
     * Keys are hardware-backed and cannot be extracted
     */
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // No PIN/biometric required
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
        
        android.util.Log.d(TAG, "Encryption key generated")
    }
    
    /**
     * Get encryption key from KeyStore
     */
    private fun getKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    /**
     * Save video to secure encrypted storage
     * Video file is encrypted and original is deleted
     * 
     * @return Encrypted file path (database reference only)
     */
    fun saveVideoSecurely(videoFile: File): SecureVideoMetadata {
        try {
            // Read original video
            val videoBytes = videoFile.readBytes()
            
            // Encrypt video
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(videoBytes)
            
            // Save to secure storage directory (database-backed)
            val secureDir = File(context.filesDir, ".secure_videos")
            if (!secureDir.exists()) {
                secureDir.mkdirs()
                // Hide directory from file browsers
                File(secureDir, ".nomedia").createNewFile()
            }
            
            // Generate secure filename (UUID-based, no readable info)
            val secureFileName = java.util.UUID.randomUUID().toString() + ".enc"
            val secureFile = File(secureDir, secureFileName)
            
            // Write encrypted video
            secureFile.writeBytes(encryptedBytes)
            
            // Delete original unencrypted file
            videoFile.delete()
            
            android.util.Log.d(TAG, "Video encrypted and saved: $secureFileName")
            
            return SecureVideoMetadata(
                encryptedPath = secureFile.absolutePath,
                encryptedFileName = secureFileName,
                iv = Base64.encodeToString(iv, Base64.NO_WRAP),
                originalSize = videoBytes.size.toLong(),
                encryptedSize = encryptedBytes.size.toLong()
            )
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to encrypt video", e)
            throw SecurityException("Video encryption failed: ${e.message}")
        }
    }
    
    /**
     * Retrieve and decrypt video (for authorized playback only)
     * Returns temporary decrypted file that is deleted after use
     */
    fun getVideoForPlayback(encryptedPath: String, iv: String): File {
        try {
            val encryptedFile = File(encryptedPath)
            
            if (!encryptedFile.exists()) {
                throw SecurityException("Encrypted video not found")
            }
            
            // Read encrypted bytes
            val encryptedBytes = encryptedFile.readBytes()
            
            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            // Create temporary file for playback
            val tempDir = File(context.cacheDir, "temp_playback")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            val tempFile = File.createTempFile("video_", ".mp4", tempDir)
            tempFile.writeBytes(decryptedBytes)
            
            android.util.Log.d(TAG, "Video decrypted for playback: ${tempFile.name}")
            
            return tempFile
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to decrypt video", e)
            throw SecurityException("Video decryption failed: ${e.message}")
        }
    }
    
    /**
     * Delete encrypted video permanently
     */
    fun deleteSecureVideo(encryptedPath: String): Boolean {
        try {
            val file = File(encryptedPath)
            val deleted = file.delete()
            
            android.util.Log.d(TAG, "Encrypted video deleted: $deleted")
            return deleted
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to delete encrypted video", e)
            return false
        }
    }
    
    /**
     * Clean up temporary decrypted files
     */
    fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "temp_playback")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { it.delete() }
            }
            android.util.Log.d(TAG, "Temp files cleaned up")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cleanup temp files", e)
        }
    }
    
    /**
     * Get all secure video files (for maintenance)
     */
    fun getSecureVideoCount(): Int {
        val secureDir = File(context.filesDir, ".secure_videos")
        return secureDir.listFiles()?.filter { it.extension == "enc" }?.size ?: 0
    }
    
    /**
     * Calculate total secure storage used
     */
    fun getTotalSecureStorage(): Long {
        val secureDir = File(context.filesDir, ".secure_videos")
        return secureDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}

/**
 * Metadata for encrypted video
 */
data class SecureVideoMetadata(
    val encryptedPath: String,
    val encryptedFileName: String,
    val iv: String, // Initialization Vector for decryption
    val originalSize: Long,
    val encryptedSize: Long
)
