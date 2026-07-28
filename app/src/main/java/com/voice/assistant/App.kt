package com.voice.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {
    companion object {
        const val TAG = "AIAssistant"
        const val CHANNEL_ID = "assistant_service"
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        // Bug #12:  appendText 
        // Bug #13: 
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val log = "[$timestamp] Thread: ${thread.name}\n$sw\n---\n"
                File(filesDir, "crash.log").appendText(log)
            } catch (_: Exception) {}
            // Bug #13: 
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannel() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = ""
                }
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createNotificationChannel failed", e)
        }
    }
}
