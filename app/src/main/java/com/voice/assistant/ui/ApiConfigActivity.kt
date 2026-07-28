package com.voice.assistant.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.voice.assistant.R
import com.voice.assistant.util.Prefs

class ApiConfigActivity : Activity() {
    companion object {
        private const val TAG = "ApiConfigActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_api_config)
            Prefs.init(this)

            val etBili = findViewById<EditText>(R.id.et_bili_uid)
            val etCity = findViewById<EditText>(R.id.et_weather_city)
            val etGithub = findViewById<EditText>(R.id.et_github_user)
            etBili?.setText(Prefs.bilibiliUid)
            etCity?.setText(Prefs.weatherCity)
            etGithub?.setText(Prefs.githubUsername)

            findViewById<Button>(R.id.btn_save_api)?.setOnClickListener {
                try {
                    Prefs.bilibiliUid = etBili?.text?.toString()?.trim() ?: ""
                    Prefs.weatherCity = etCity?.text?.toString()?.trim() ?: ""
                    Prefs.githubUsername = etGithub?.text?.toString()?.trim() ?: ""
                    Toast.makeText(this, " API", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "save failed", e)
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
