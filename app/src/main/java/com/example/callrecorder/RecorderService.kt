package com.example.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaRecorder
import android.net.Uri
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderService : Service() {
    companion object {
        const val START = "start"
        const val STOP = "stop"
        @Volatile var recording = false
    }

    private var recorder: MediaRecorder? = null
    private var pfd: ParcelFileDescriptor? = null
    private var uri: Uri? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            end(); stopSelf()
        } else if (!recording) {
            begin()
        }
        return START_NOT_STICKY
    }

    private fun begin() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("rec", "Recording", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 0, Intent(this, RecorderService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "rec")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recording…")
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Stop", stop
                ).build()
            )
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        try {
            val name = "call_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/CallRecorder")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val u = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
            uri = u
            pfd = contentResolver.openFileDescriptor(u, "w")
            recorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(pfd!!.fileDescriptor)
                prepare()
                start()
            }
            recording = true
        } catch (e: Exception) {
            end(); stopSelf()
        }
    }

    private fun end() {
        recording = false
        var ok = true
        try { recorder?.stop() } catch (e: RuntimeException) { ok = false }
        recorder?.release(); recorder = null
        pfd?.close(); pfd = null
        uri?.let {
            if (ok) {
                contentResolver.update(it, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } else {
                contentResolver.delete(it, null, null)
            }
        }
        uri = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        end()
        super.onDestroy()
    }
}
