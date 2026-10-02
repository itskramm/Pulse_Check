package com.example.pulsecheck

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class to record 5-second video segments during SOS alerts
 * 
 * Features:
 * - Records from back camera (captures what user sees)
 * - Auto-creates 5-second video segments while button is held
 * - Saves each segment to app's private storage
 * - Automatically starts next segment if still recording
 * - Background recording (no preview needed)
 * 
 * Usage:
 * val recorder = VideoRecorderHelper(context)
 * recorder.startRecording { videoFile ->
 *     // Called when each 5-second segment completes
 * }
 * // While button is held, new segments auto-start
 * recorder.stopRecording() // Stops current segment
 */
class VideoRecorderHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoRecorderHelper"
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 2_000_000 // 2 Mbps
        private const val SEGMENT_DURATION_MS = 5000 // 5 seconds per segment
    }
    
    private var cameraDevice: CameraDevice? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingSession: CameraCaptureSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRecording = false
    private var shouldContinueRecording = false // Track if user still holding button
    private var videoFile: File? = null
    private var recordingCallback: RecordingCallback? = null
    private var segmentCount = 0
    
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onSegmentComplete(videoFile: File, segmentNumber: Int)
        fun onRecordingFailed(error: String)
    }
    
    /**
     * Check if camera permission is granted
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start 5-second segment recording
     * Automatically creates new segments every 5 seconds while shouldContinueRecording is true
     */
    fun startRecording(callback: RecordingCallback) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        if (!hasPermission()) {
            callback.onRecordingFailed("Camera permission not granted")
            return
        }
        
        recordingCallback = callback
        shouldContinueRecording = true
        segmentCount = 0
        
        startNextSegment()
    }
    
    /**
     * Start recording the next 5-second segment
     */
    private fun startNextSegment() {
        if (!shouldContinueRecording) {
            Log.d(TAG, "Not continuing to next segment (user released button)")
            return
        }
        
        segmentCount++
        
        try {
            // Create video file for this segment
            videoFile = createVideoFile(segmentCount)
            
            // Setup media recorder
            setupMediaRecorder(videoFile!!)
            
            // Open camera and start recording (or reuse if already open)
            if (cameraDevice == null) {
                openCamera(recordingCallback!!)
            } else {
                // Camera already open, just start new recording session
                startRecordingSession(cameraDevice!!, recordingCallback!!)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start segment recording", e)
            recordingCallback?.onRecordingFailed(e.message ?: "Unknown error")
            cleanup()
        }
    }
    
    /**
     * Stop recording - user released button
     * Stops current segment and prevents new segments from starting
     */
    fun stopRecording() {
        shouldContinueRecording = false // Stop creating new segments
        
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return
        }
        
        stopCurrentSegment()
    }
    
    /**
     * Stop the current 5-second segment
     */
    private fun stopCurrentSegment() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.stop()
            Log.d(TAG, "Segment $segmentCount stopped")
            
            // Notify callback with the saved video file
            val savedFile = videoFile
            if (savedFile != null && savedFile.exists()) {
                recordingCallback?.onSegmentComplete(savedFile, segmentCount)
            } else {
                Log.e(TAG, "Video file not found after stopping segment")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping segment", e)
            recordingCallback?.onRecordingFailed("Error stopping segment: ${e.message}")
        } finally {
            // Clean up recorder but keep camera open if continuing
            mediaRecorder?.release()
            mediaRecorder = null
            recordingSession?.close()
            recordingSession = null
            isRecording = false
            
            // If still holding button, start next segment
            if (shouldContinueRecording) {
                handler.postDelayed({
                    startNextSegment()
                }, 100) // Small delay to ensure cleanup
            } else {
                // User released button, clean up everything
                cleanup()
            }
        }
    }
    
    /**
     * Create video file in app's private directory
     * Each segment gets a unique number
     */
    private fun createVideoFile(segmentNumber: Int): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "SOS_VIDEO_${timestamp}_seg${segmentNumber}.mp4"
        
        // Save to app's private files directory
        val videosDir = File(context.filesDir, "sos_videos")
        if (!videosDir.exists()) {
            videosDir.mkdirs()
        }
        
        return File(videosDir, fileName)
    }
    
    /**
     * Setup MediaRecorder with video parameters for 5-second segment
     */
    private fun setupMediaRecorder(outputFile: File) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoEncodingBitRate(VIDEO_BIT_RATE)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            
            // Set max duration to exactly 5 seconds
            setMaxDuration(SEGMENT_DURATION_MS)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d(TAG, "5-second segment complete")
                    // Stop this segment and start next if still holding
                    handler.post {
                        stopCurrentSegment()
                    }
                }
            }
            
            prepare()
        }
    }
    
    /**
     * Open camera and start recording
     */
    private fun openCamera(callback: RecordingCallback) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try {
            // Get back camera ID
            val cameraId = getBackCameraId(cameraManager)
            if (cameraId == null) {
                callback.onRecordingFailed("Back camera not found")
                return
            }
            
            // Check permission again (required by Android)
            if (!hasPermission()) {
                callback.onRecordingFailed("Camera permission denied")
                return
            }
            
            // Open camera
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startRecordingSession(camera, callback)
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    camera.close()
                    cameraDevice = null
                    callback.onRecordingFailed("Camera disconnected")
                    cleanup()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                    callback.onRecordingFailed("Camera error: $error")
                    cleanup()
                }
            }, handler)
            
        } catch (e: SecurityException) {
            callback.onRecordingFailed("Camera permission denied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
            callback.onRecordingFailed(e.message ?: "Failed to open camera")
        }
    }
    
    /**
     * Start camera capture session for recording
     */
    private fun startRecordingSession(camera: CameraDevice, callback: RecordingCallback) {
        try {
            val surface = mediaRecorder?.surface
            if (surface == null) {
                callback.onRecordingFailed("MediaRecorder surface is null")
                return
            }
            
            // Create capture request
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }
            
            // Create capture session
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        recordingSession = session
                        
                        try {
                            // Start repeating request
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                handler
                            )
                            
                            // Start recording
                            mediaRecorder?.start()
                            isRecording = true
                            
                            Log.d(TAG, "Recording segment $segmentCount started: ${videoFile?.name}")
                            callback.onRecordingStarted()
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start recording", e)
                            callback.onRecordingFailed(e.message ?: "Failed to start recording")
                            cleanup()
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        callback.onRecordingFailed("Failed to configure camera")
                        cleanup()
                    }
                },
                handler
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create recording session", e)
            callback.onRecordingFailed(e.message ?: "Session creation failed")
            cleanup()
        }
    }
    
    /**
     * Get back camera ID
     */
    private fun getBackCameraId(cameraManager: CameraManager): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting back camera", e)
            null
        }
    }
    
    /**
     * Clean up resources
     */
    private fun cleanup() {
        isRecording = false
        shouldContinueRecording = false
        segmentCount = 0
        
        recordingSession?.close()
        recordingSession = null
        
        mediaRecorder?.release()
        mediaRecorder = null
        
        cameraDevice?.close()
        cameraDevice = null
    }
    
    /**
     * Get all recorded videos
     */
    fun getAllVideos(): List<File> {
        val videosDir = File(context.filesDir, "sos_videos")
        if (!videosDir.exists()) return emptyList()
        
        return videosDir.listFiles()?.filter { it.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Delete video file
     */
    fun deleteVideo(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete video", e)
            false
        }
    }
    
    /**
     * Delete all videos older than specified days
     */
    fun deleteOldVideos(daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        getAllVideos().filter { it.lastModified() < cutoffTime }.forEach { deleteVideo(it) }
    }
}
