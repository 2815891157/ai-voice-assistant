package com.voice.assistant.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ApiEngine {

    private var packageManager: PackageManager? = null
    // Bug #18:  lowercase map 
    private var installedApps: MutableMap<String, String> = mutableMapOf()

    fun init(context: Context) {
        packageManager = context.applicationContext.packageManager
        refreshAppList()
    }

    fun refreshAppList() {
        val pm = packageManager ?: return
        installedApps.clear()
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (info in resolveInfos) {
            val appName = info.loadLabel(pm).toString()
            val pkgName = info.activityInfo.packageName
            // Bug #18:  lowercase key
            installedApps[appName.lowercase()] = pkgName
        }
    }

    //   
    // Bug #4:  use {} 
    private fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
        } catch (e: Exception) {
            null
        }
    }

    fun process(input: String, context: Context? = null): Pair<String, String?> {
        val cmd = input.trim()
        val lower = cmd.lowercase()

        Prefs.findReply(input)?.let { return Pair(it, null) }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("") || lower.contains("") || lower.contains("") || lower.contains("")) {
            val songName = cmd.replace(Regex("||||||||"), "").trim()
            if (songName.isNotEmpty()) return searchAndPlayMusic(songName)
            return Pair("~", null)
        }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("")) {
            val appKeyword = cmd.replace(Regex("||||"), "").trim().lowercase()
            if (appKeyword.isNotEmpty()) {
                // 
                installedApps[appKeyword]?.let { return Pair("${appKeyword}", "LAUNCH:$it") }
                // 
                for ((name, pkg) in installedApps) {
                    if (appKeyword in name || name in appKeyword) return Pair("$name", "LAUNCH:$pkg")
                }
                // 
                if (appKeyword.contains(".")) return Pair("$appKeyword", "LAUNCH:$appKeyword")
                return Pair("\"$appKeyword\"", null)
            }
        }

        //  — Bug #11: 
        if (lower.contains("")) {
            // 
            val customLocations = Prefs.getLocations()
            for ((name, lat, lon) in customLocations) {
                if (cmd.contains(name)) return Pair(queryWeatherByCoords(lat, lon, name), null)
            }
            // Bug #24: ""
            val weatherIdx = lower.indexOf("")
            if (weatherIdx > 0) {
                val prefix = cmd.substring(0, weatherIdx)
                    .replace(Regex("||||||||||||"), "")
                    .trim()
                if (prefix.isNotEmpty()) return Pair(queryWeatherByCity(prefix), null)
            }
            //  GPS 
            val location = Prefs.getLastLocation()
            if (location != null) return Pair(queryWeatherByCoords(location.first, location.second, ""), null)
            //  fallback 
            if (Prefs.weatherCity.isNotEmpty()) return Pair(queryWeatherByCity(Prefs.weatherCity), null)
            return Pair("GPS", null)
        }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("")) {
            return Pair("${SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())}", null)
        }
        if (lower.contains("") || lower.contains("") || lower.contains("")) {
            return Pair("${SimpleDateFormat("yyyyMMdd EEEE", Locale.CHINESE).format(Date())}", null)
        }
        if (lower.contains("") || lower.contains("")) {
            return Pair("${SimpleDateFormat("EEEE", Locale.CHINESE).format(Date())}", null)
        }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("") || lower.matches(Regex(".*\\d+.*[+\\-×÷*/].*\\d+.*"))) {
            val result = tryCalculate(cmd)
            if (result != null) return Pair("$result", null)
        }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("")) return Pair(queryNews(), null)

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("") ||
            lower.contains("") || lower.contains("") || lower.contains("") || lower.contains("")) {
            val query = cmd.replace(Regex("||||||||"), "").trim()
            if (query.isNotEmpty()) return Pair(searchBaike(query), null)
        }

        // 
        if (lower.contains("") || lower.contains("") || lower.contains("") || lower.contains("")) {
            val query = cmd.replace(Regex("||||"), "").trim()
            if (query.isNotEmpty()) return Pair(searchWeb(query), null)
        }

        // 
        if (lower.contains("") || lower.contains("")) return Pair("AI~", null)
        if (lower.contains("") || lower.contains("") || lower.contains("")) return Pair(buildHelpText(), null)
        if (lower.contains("")) return Pair("~ ", null)
        if (lower.contains("") || lower.contains("")) return Pair("~ ", null)
        if (lower.contains("") || lower.contains("")) return Pair("~ ", null)
        if (lower.contains("")) return Pair("~ ", null)

        // 
        if (lower.contains("") || lower.contains("")) return Pair("~", "LAUNCH:com.android.deskclock")
        if (lower.contains("") || lower.contains("")) return Pair("~", "LAUNCH:com.android.dialer")
        if (lower.contains("") || lower.contains("")) return Pair("~", "LAUNCH:com.android.camera")
        if (lower.contains("")) return Pair("~", "LAUNCH:com.autonavi.minimap")
        if (lower.contains("")) return Pair("~", "OPEN_SETTINGS")

        // B
        if (lower.contains("") && (lower.contains("") || lower.contains("b") || lower.contains("bilibili"))) {
            return Pair(queryBiliFans(), null)
        }

        return Pair("\n• \"\" — \n• \"\" — \n• \"\" — \n• \"\" — \n• \"\" — \n• \"\"", null)
    }

    //   
    private fun searchAndPlayMusic(songName: String): Pair<String, String?> {
        val encoded = URLEncoder.encode(songName, "UTF-8")
        val body = httpGet("https://music.163.com/api/search/get/web?s=$encoded&type=1&limit=5&offset=0",
            mapOf("Referer" to "https://music.163.com")) ?: return Pair("", null)
        return try {
            val json = JSONObject(body)
            val songs = json.getJSONObject("result").getJSONArray("songs")
            if (songs.length() == 0) return Pair("\"$songName\"~", null)
            val song = songs.getJSONObject(0)
            val songId = song.getLong("id")
            val name = song.getString("name")
            val artists = song.getJSONArray("artists")
            val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else ""
            Pair(" $name - $artistName", "MUSIC_ORPHEUS:$songId")
        } catch (e: Exception) { Pair("${e.message}", null) }
    }

    //   
    private fun queryWeatherByCity(city: String): String {
        val body = httpGet("https://wttr.in/${URLEncoder.encode(city, "UTF-8")}?format=j1&lang=zh",
            mapOf("User-Agent" to "curl/7.68.0")) ?: return ""
        return try {
            val json = JSONObject(body)
            val cond = json.getJSONArray("current_condition").getJSONObject(0)
            val temp = cond.getString("temp_C")
            val feels = cond.getString("FeelsLikeC")
            val hum = cond.getString("humidity")
            var desc = ""
            if (cond.has("lang_zh")) desc = cond.getJSONArray("lang_zh").getJSONObject(0).getString("value")
            "${city}${desc}${temp}°C${feels}°C${hum}%"
        } catch (e: Exception) { "${e.message}" }
    }

    private fun queryWeatherByCoords(lat: Double, lon: Double, name: String): String {
        val body = httpGet("https://wttr.in/$lat,$lon?format=j1&lang=zh",
            mapOf("User-Agent" to "curl/7.68.0")) ?: return ""
        return try {
            val json = JSONObject(body)
            val cond = json.getJSONArray("current_condition").getJSONObject(0)
            val temp = cond.getString("temp_C")
            val feels = cond.getString("FeelsLikeC")
            val hum = cond.getString("humidity")
            var desc = ""
            if (cond.has("lang_zh")) desc = cond.getJSONArray("lang_zh").getJSONObject(0).getString("value")
            "${name}${desc}${temp}°C${feels}°C${hum}%"
        } catch (e: Exception) { "${e.message}" }
    }

    //   
    private fun queryNews(): String {
        val body = httpGet("https://hacker-news.firebaseio.com/v0/topstories.json") ?: return ""
        return try {
            val ids = JSONArray(body)
            val sb = StringBuilder(" \n")
            val count = minOf(ids.length(), 5)
            for (i in 0 until count) {
                val storyBody = httpGet("https://hacker-news.firebaseio.com/v0/item/${ids.getLong(i)}.json")
                if (storyBody != null) {
                    val title = JSONObject(storyBody).optString("title", "")
                    sb.append("${i + 1}. $title\n")
                }
            }
            sb.toString().trim()
        } catch (e: Exception) { "${e.message}" }
    }

    //   
    private fun searchBaike(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = httpGet("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1") ?: return ""
        return try {
            val json = JSONObject(body)
            val abstract = json.optString("Abstract", "")
            val source = json.optString("AbstractSource", "")
            if (abstract.isNotEmpty()) " $abstract\n—— $source"
            else {
                val related = json.optJSONArray("RelatedTopics")
                if (related != null && related.length() > 0) {
                    val text = related.getJSONObject(0).optString("Text", "")
                    if (text.isNotEmpty()) " $text" else "\"$query\""
                } else "\"$query\""
            }
        } catch (e: Exception) { "${e.message}" }
    }

    //   
    // Bug #19: 
    private fun searchWeb(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = httpGet("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1") ?: return ""
        return try {
            val json = JSONObject(body)
            val abstract = json.optString("Abstract", "")
            val heading = json.optString("Heading", "")
            if (abstract.isNotEmpty()) " $heading\n$abstract"
            else {
                val related = json.optJSONArray("RelatedTopics")
                if (related != null && related.length() > 0) {
                    val sb = StringBuilder(" \"$query\"\n")
                    val count = minOf(related.length(), 3)
                    for (i in 0 until count) {
                        val text = related.getJSONObject(i).optString("Text", "")
                        if (text.isNotEmpty()) sb.append("• $text\n")
                    }
                    sb.toString().trim()
                } else "\"$query\""
            }
        } catch (e: Exception) { "${e.message}" }
    }

    //  B 
    private fun queryBiliFans(): String {
        val uid = Prefs.bilibiliUid
        if (uid.isEmpty()) return "BUID-API"
        val body = httpGet("https://api.bilibili.com/x/relation/stat?vmid=$uid") ?: return "B"
        return try {
            val data = JSONObject(body).getJSONObject("data")
            "B ${formatNumber(data.getLong("follower"))} "
        } catch (e: Exception) { "${e.message}" }
    }

    //   
    private fun tryCalculate(input: String): String? {
        val cleaned = input
            .replace(Regex("[]"), "")
            .replace("×", "*").replace("x", "*").replace("X", "*")
            .replace("÷", "/").replace("", "/")
            .replace("", "+").replace("", "-").replace("", "*").replace("", "*")
            .trim().replace(Regex("\\s*"), "")
        try {
            if (cleaned.matches(Regex("[\\d+\\-*/.()]+"))) {
                val result = evaluateExpression(cleaned)
                return if (result == result.toLong().toDouble()) result.toLong().toString()
                else "%.6f".format(result).trimEnd('0').trimEnd('.')
            }
        } catch (_: Exception) {}
        return null
    }

    private fun evaluateExpression(expr: String): Double {
        var pos = 0
        fun peek(): Char? = if (pos < expr.length) expr[pos] else null
        fun next(): Char = expr[pos++]
        fun parseFactor(): Double {
            while (peek() == ' ') pos++
            if (peek() == '(') { next(); val v = parseAddSub(); while (peek() == ' ') pos++; if (peek() == ')') next(); return v }
            if (peek() == '-') { next(); return -parseFactor() }
            val start = pos
            while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++
            return expr.substring(start, pos).toDoubleOrNull() ?: 0.0
        }
        fun parseMulDiv(): Double {
            var left = parseFactor()
            while (true) { while (peek() == ' ') pos++; val op = peek() ?: break
                if (op == '*' || op == '/') { next(); val right = parseFactor(); left = if (op == '*') left * right else { if (right == 0.0) throw ArithmeticException("/0"); left / right } } else break }
            return left
        }
        fun parseAddSub(): Double {
            var left = parseMulDiv()
            while (true) { while (peek() == ' ') pos++; val op = peek() ?: break
                if (op == '+' || op == '-') { next(); val right = parseMulDiv(); left = if (op == '+') left + right else left - right } else break }
            return left
        }
        return parseAddSub()
    }

    private fun formatNumber(n: Long): String = when {
        n >= 100000000 -> "${"%.1f".format(n / 1e8)}"
        n >= 10000 -> "${"%.1f".format(n / 1e4)}"
        else -> n.toString()
    }

    private fun buildHelpText(): String = "\n  — \"\"\n  — \"\"\n  — \"\"\n  — \"\"\n  — \"\"\n  — \"xxx\"\n  — \"10025\"\n  — \"\"\n B — \"B\""
}
