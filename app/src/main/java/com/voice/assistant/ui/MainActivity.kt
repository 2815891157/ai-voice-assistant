package com.voice.assistant.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.voice.assistant.R
import com.voice.assistant.FloatingService
import com.voice.assistant.util.ApiEngine
import com.voice.assistant.util.Prefs
import java.io.File

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQ_OVERLAY = 1001
        private const val REQ_PERMISSIONS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            Prefs.init(this)
            ApiEngine.init(this)

            findViewById<Button>(R.id.btn_start)?.setOnClickListener { checkAndStart() }
            findViewById<Button>(R.id.btn_settings)?.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            findViewById<Button>(R.id.btn_api)?.setOnClickListener {
                startActivity(Intent(this, ApiConfigActivity::class.java))
            }
            findViewById<Button>(R.id.btn_custom)?.setOnClickListener {
                startActivity(Intent(this, CustomReplyActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            Toast.makeText(this, ": ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndStart() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY)
                return
            }
            val perms = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.BODY_SENSORS,
            )
            if (Build.VERSION.SDK_INT >= 33) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            val needed = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
            if (needed.isNotEmpty()) {
                requestPermissions(needed, REQ_PERMISSIONS)
                return
            }
            startAssistantService()
        } catch (e: Exception) {
            Log.e(TAG, "checkAndStart error", e)
            Toast.makeText(this, ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAssistantService() {
        try {
            val intent = Intent(this, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "startService error", e)
            Toast.makeText(this, ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == REQ_PERMISSIONS) {
            // Bug #2: 
            val criticalPerms = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION)
            val allGranted = criticalPerms.all { perm ->
                perms.indexOf(perm).let { idx -> idx >= 0 && res[idx] == PackageManager.PERMISSION_GRANTED }
            }
            if (allGranted) {
                startAssistantService()
            } else {
                Toast.makeText(this, "", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(code: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(code, resultCode, data)
        if (code == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this))
                checkAndStart()
            else Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
        }
    }
}
