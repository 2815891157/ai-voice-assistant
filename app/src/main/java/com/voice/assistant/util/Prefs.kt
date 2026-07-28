package com.voice.assistant.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NamedLocation(val name: String, val lat: Double, val lon: Double)

object Prefs {
    private const val TAG = "Prefs"
    private const val NAME = "ai_assistant_prefs"
    @Volatile
    private var sp: SharedPreferences? = null

    @Synchronized
    fun init(context: Context) {
        try {
            if (sp == null) {
                sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Prefs.init failed", e)
        }
    }

    private fun get(): SharedPreferences {
        if (sp == null) throw IllegalStateException("Prefs not initialized")
        return sp!!
    }

    //   
    var avatarPath: String
        get() = try { get().getString("avatar", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("avatar", v).apply() } catch (_: Exception) {} }

    var customAvatarPath: String
        get() = try { get().getString("custom_avatar", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("custom_avatar", v).apply() } catch (_: Exception) {} }

    fun avatarFile(context: Context): File? {
        return try {
            val path = if (customAvatarPath.isNotEmpty()) customAvatarPath else avatarPath
            val f = File(path)
            if (f.exists()) f else null
        } catch (_: Exception) { null }
    }

    //   
    var wakeWord: String
        get() = try { get().getString("wake_word", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("wake_word", v).apply() } catch (_: Exception) {} }

    var wakeWordEnabled: Boolean
        get() = try { get().getBoolean("wake_enabled", true) } catch (_: Exception) { true }
        set(v) { try { get().edit().putBoolean("wake_enabled", v).apply() } catch (_: Exception) {} }

    //  TTS 
    var ttsLang: String
        get() = try { get().getString("tts_lang", "zh-CN") ?: "zh-CN" } catch (_: Exception) { "zh-CN" }
        set(v) { try { get().edit().putString("tts_lang", v).apply() } catch (_: Exception) {} }

    var ttsSpeed: Float
        get() = try { get().getFloat("tts_speed", 1.0f) } catch (_: Exception) { 1.0f }
        set(v) { try { get().edit().putFloat("tts_speed", v).apply() } catch (_: Exception) {} }

    var ttsPitch: Float
        get() = try { get().getFloat("tts_pitch", 1.0f) } catch (_: Exception) { 1.0f }
        set(v) { try { get().edit().putFloat("tts_pitch", v).apply() } catch (_: Exception) {} }

    //   
    var greeting: String
        get() = try { get().getString("greeting", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("greeting", v).apply() } catch (_: Exception) {} }

    var alwaysRespond: Boolean
        get() = try { get().getBoolean("always_respond", false) } catch (_: Exception) { false }
        set(v) { try { get().edit().putBoolean("always_respond", v).apply() } catch (_: Exception) {} }

    var bubbleTimeout: Int
        get() = try { get().getInt("bubble_timeout", 5) } catch (_: Exception) { 5 }
        set(v) { try { get().edit().putInt("bubble_timeout", v).apply() } catch (_: Exception) {} }

    var hapticFeedback: Boolean
        get() = try { get().getBoolean("haptic_feedback", true) } catch (_: Exception) { true }
        set(v) { try { get().edit().putBoolean("haptic_feedback", v).apply() } catch (_: Exception) {} }

    //   
    var useGpsLocation: Boolean
        get() = try { get().getBoolean("use_gps", true) } catch (_: Exception) { true }
        set(v) { try { get().edit().putBoolean("use_gps", v).apply() } catch (_: Exception) {} }

    private var lastLat: String
        get() = try { get().getString("last_lat", "0") ?: "0" } catch (_: Exception) { "0" }
        set(v) { try { get().edit().putString("last_lat", v).apply() } catch (_: Exception) {} }

    private var lastLon: String
        get() = try { get().getString("last_lon", "0") ?: "0" } catch (_: Exception) { "0" }
        set(v) { try { get().edit().putString("last_lon", v).apply() } catch (_: Exception) {} }

    fun saveLastLocation(lat: Double, lon: Double) {
        lastLat = lat.toString()
        lastLon = lon.toString()
    }

    fun getLastLocation(): Pair<Double, Double>? {
        val lat = lastLat.toDoubleOrNull() ?: 0.0
        val lon = lastLon.toDoubleOrNull() ?: 0.0
        return if (lat != 0.0 && lon != 0.0) Pair(lat, lon) else null
    }

    //   
    fun getLocations(): List<NamedLocation> {
        return try {
            val json = get().getString("locations", "[]") ?: "[]"
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                NamedLocation(obj.getString("name"), obj.getDouble("lat"), obj.getDouble("lon"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addLocation(name: String, lat: Double, lon: Double) {
        try {
            val arr = JSONArray(get().getString("locations", "[]") ?: "[]")
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("lat", lat)
            obj.put("lon", lon)
            arr.put(obj)
            get().edit().putString("locations", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun removeLocation(name: String) {
        try {
            val arr = JSONArray(get().getString("locations", "[]") ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("name") != name) newArr.put(obj)
            }
            get().edit().putString("locations", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    //  API  
    var bilibiliUid: String
        get() = try { get().getString("bili_uid", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("bili_uid", v).apply() } catch (_: Exception) {} }

    var weatherCity: String
        get() = try { get().getString("weather_city", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("weather_city", v).apply() } catch (_: Exception) {} }

    var githubUsername: String
        get() = try { get().getString("github_user", "") ?: "" } catch (_: Exception) { "" }
        set(v) { try { get().edit().putString("github_user", v).apply() } catch (_: Exception) {} }

    //   
    fun customReplies(): Map<String, String> {
        return try {
            val json = get().getString("replies", "{}") ?: "{}"
            val result = mutableMapOf<String, String>()
            val obj = JSONObject(json)
            for (key in obj.keys()) { result[key] = obj.getString(key) }
            result
        } catch (_: Exception) { emptyMap() }
    }

    fun saveCustomReplies(map: Map<String, String>) {
        try {
            val obj = JSONObject()
            for ((k, v) in map) { obj.put(k, v) }
            get().edit().putString("replies", obj.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addReply(trigger: String, reply: String) {
        val map = customReplies().toMutableMap()
        map[trigger] = reply
        saveCustomReplies(map)
    }

    fun removeReply(trigger: String) {
        val map = customReplies().toMutableMap()
        map.remove(trigger)
        saveCustomReplies(map)
    }

    fun findReply(input: String): String? {
        for ((k, v) in customReplies()) {
            if (input.contains(k, ignoreCase = true)) return v
        }
        return null
    }

    //   
    fun getNotes(): List<String> {
        return try {
            val json = get().getString("notes", "[]") ?: "[]"
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun addNote(text: String) {
        try {
            val arr = JSONArray(get().getString("notes", "[]") ?: "[]")
            arr.put("${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())} $text")
            get().edit().putString("notes", arr.toString()).apply()
        } catch (_: Exception) {}
    }
}
