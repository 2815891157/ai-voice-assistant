package com.voice.assistant.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.voice.assistant.R
import com.voice.assistant.util.Prefs
import java.io.File

class SettingsActivity : Activity() {

    companion object {
        private const val TAG = "SettingsActivity"
        private const val PICK_IMAGE = 2001
    }

    private lateinit var llLocations: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
            Prefs.init(this)
            llLocations = findViewById(R.id.ll_locations)

            // 
            findViewById<ImageView>(R.id.iv_avatar)?.apply {
                val f = Prefs.avatarFile(this@SettingsActivity)
                if (f != null) setImageURI(Uri.fromFile(f))
            }
            findViewById<Button>(R.id.btn_pick_avatar)?.setOnClickListener {
                try {
                    startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, PICK_IMAGE)
                } catch (e: Exception) { Log.e(TAG, "pick image failed", e) }
            }

            // 
            findViewById<EditText>(R.id.et_wake_word)?.setText(Prefs.wakeWord)
            findViewById<CheckBox>(R.id.cb_wake)?.isChecked = Prefs.wakeWordEnabled

            // TTS
            val langCodes = arrayOf("zh-CN", "en-US", "ja-JP", "ko-KR")
            val langNames = arrayOf("", "English", "", "")
            val spinner = findViewById<Spinner>(R.id.sp_lang)
            spinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langNames)
            spinner?.setSelection(langCodes.indexOf(Prefs.ttsLang).coerceAtLeast(0))

            val sbSpeed = findViewById<SeekBar>(R.id.sb_speed)
            val tvSpeed = findViewById<TextView>(R.id.tv_speed_val)
            sbSpeed?.progress = ((Prefs.ttsSpeed - 0.5f) * 20).toInt()
            tvSpeed?.text = "%.1fx".format(Prefs.ttsSpeed)
            sbSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, from: Boolean) { tvSpeed?.text = "%.1fx".format(0.5f + p / 20f) }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })

            val sbPitch = findViewById<SeekBar>(R.id.sb_pitch)
            val tvPitch = findViewById<TextView>(R.id.tv_pitch_val)
            sbPitch?.progress = ((Prefs.ttsPitch - 0.5f) * 20).toInt()
            tvPitch?.text = "%.1fx".format(Prefs.ttsPitch)
            sbPitch?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, from: Boolean) { tvPitch?.text = "%.1fx".format(0.5f + p / 20f) }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })

            // 
            val sbBubble = findViewById<SeekBar>(R.id.sb_bubble_timeout)
            val tvBubble = findViewById<TextView>(R.id.tv_bubble_timeout_val)
            sbBubble?.progress = (Prefs.bubbleTimeout - 2).coerceIn(0, 8)
            tvBubble?.text = "${Prefs.bubbleTimeout}"
            sbBubble?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, from: Boolean) { tvBubble?.text = "${p + 2}" }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })

            // 
            findViewById<CheckBox>(R.id.cb_use_gps)?.isChecked = Prefs.useGpsLocation
            loadLocations()

            findViewById<Button>(R.id.btn_add_location)?.setOnClickListener {
                val name = findViewById<EditText>(R.id.et_location_name)?.text?.toString()?.trim() ?: ""
                val latStr = findViewById<EditText>(R.id.et_location_lat)?.text?.toString()?.trim() ?: ""
                val lonStr = findViewById<EditText>(R.id.et_location_lon)?.text?.toString()?.trim() ?: ""
                if (name.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val lat = latStr.toDoubleOrNull()
                val lon = lonStr.toDoubleOrNull()
                if (lat == null || lon == null) {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Prefs.addLocation(name, lat, lon)
                loadLocations()
                findViewById<EditText>(R.id.et_location_name)?.text?.clear()
                findViewById<EditText>(R.id.et_location_lat)?.text?.clear()
                findViewById<EditText>(R.id.et_location_lon)?.text?.clear()
                Toast.makeText(this, " \"$name\"", Toast.LENGTH_SHORT).show()
            }

            // 
            findViewById<EditText>(R.id.et_greeting)?.setText(Prefs.greeting)

            // 
            findViewById<Button>(R.id.btn_save)?.setOnClickListener {
                try {
                    Prefs.wakeWord = findViewById<EditText>(R.id.et_wake_word)?.text?.toString()?.trim() ?: ""
                    Prefs.wakeWordEnabled = findViewById<CheckBox>(R.id.cb_wake)?.isChecked ?: true
                    Prefs.ttsLang = langCodes[spinner?.selectedItemPosition ?: 0]
                    Prefs.ttsSpeed = 0.5f + (sbSpeed?.progress ?: 10) / 20f
                    Prefs.ttsPitch = 0.5f + (sbPitch?.progress ?: 10) / 20f
                    Prefs.alwaysRespond = findViewById<CheckBox>(R.id.cb_always_respond)?.isChecked ?: false
                    Prefs.bubbleTimeout = (sbBubble?.progress ?: 3) + 2
                    Prefs.useGpsLocation = findViewById<CheckBox>(R.id.cb_use_gps)?.isChecked ?: true
                    Prefs.greeting = findViewById<EditText>(R.id.et_greeting)?.text?.toString()?.trim() ?: ""
                    Toast.makeText(this, " ", Toast.LENGTH_SHORT).show()
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

    private fun loadLocations() {
        llLocations.removeAllViews()
        val locations = Prefs.getLocations()
        for (loc in locations) {
            val inflater = LayoutInflater.from(this)
            val item = inflater.inflate(R.layout.item_reply, llLocations, false)
            item.findViewById<TextView>(R.id.tv_trigger)?.text = loc.name
            item.findViewById<TextView>(R.id.tv_reply)?.text = "(${loc.lat}, ${loc.lon})"
            item.findViewById<ImageButton>(R.id.btn_delete)?.setOnClickListener {
                Prefs.removeLocation(loc.name)
                loadLocations()
            }
            llLocations.addView(item)
        }
    }

    override fun onActivityResult(code: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(code, resultCode, data)
        if (code == PICK_IMAGE && resultCode == RESULT_OK && data?.data != null) {
            try {
                // Bug #14:  256x256
                val input = contentResolver.openInputStream(data.data!!) ?: return
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                input.close()
                if (bitmap != null) {
                    val size = 256
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, size, size, true)
                    val file = File(filesDir, "avatar.png")
                    file.outputStream().use { out ->
                        scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 85, out)
                    }
                    bitmap.recycle()
                    scaled.recycle()
                    Prefs.avatarPath = file.absolutePath
                    Prefs.customAvatarPath = file.absolutePath
                    findViewById<ImageView>(R.id.iv_avatar)?.setImageURI(Uri.fromFile(file))
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Log.e(TAG, "copyAvatar failed", e) }
        }
    }
}
