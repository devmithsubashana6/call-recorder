package com.example.callrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var btn: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.setOnApplyWindowInsetsListener { v, ins ->
            val s = ins.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(s.left + pad, s.top + pad, s.right + pad, s.bottom + pad)
            ins
        }
        val info = TextView(this).apply {
            textSize = 16f
            text = "1. Tap Start.\n" +
                "2. Make or answer the call and switch to speakerphone so the mic can hear both sides.\n" +
                "3. Tap Stop (here or in the notification) when finished.\n\n" +
                "Files are saved in Recordings/CallRecorder.\n\n" +
                "Only record calls when everyone on the line has agreed and it is legal where you live."
        }
        status = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, pad * 2, 0, pad)
        }
        btn = Button(this).apply { setOnClickListener { toggle() } }
        root.addView(info)
        root.addView(status)
        root.addView(btn)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render(RecorderService.recording)
    }

    private fun toggle() {
        if (RecorderService.recording) {
            startService(Intent(this, RecorderService::class.java).setAction(RecorderService.STOP))
            render(false)
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1
            )
        } else {
            startForegroundService(Intent(this, RecorderService::class.java).setAction(RecorderService.START))
            render(true)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) toggle()
    }

    private fun render(on: Boolean) {
        btn.text = if (on) "Stop recording" else "Start recording"
        status.text = if (on) "● Recording" else "Idle"
    }
}
