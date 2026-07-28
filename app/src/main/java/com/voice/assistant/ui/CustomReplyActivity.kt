package com.voice.assistant.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.voice.assistant.R
import com.voice.assistant.util.Prefs

class CustomReplyActivity : Activity() {

    companion object {
        private const val TAG = "CustomReplyActivity"
    }

    private var llReplies: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_custom_reply)
            Prefs.init(this)

            llReplies = findViewById(R.id.ll_replies)
            loadReplies()

            val etTrigger = findViewById<EditText>(R.id.et_trigger)
            val etReply = findViewById<EditText>(R.id.et_reply)

            findViewById<Button>(R.id.btn_add)?.setOnClickListener {
                try {
                    val t = etTrigger?.text?.toString()?.trim() ?: ""
                    val r = etReply?.text?.toString()?.trim() ?: ""
                    if (t.isEmpty() || r.isEmpty()) {
                        Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    Prefs.addReply(t, r)
                    addItem(t, r)
                    etTrigger?.text?.clear()
                    etReply?.text?.clear()
                } catch (e: Exception) {
                    Log.e(TAG, "add reply failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadReplies() {
        try {
            llReplies?.removeAllViews()
            val map = Prefs.customReplies()
            for ((k, v) in map) addItem(k, v)
        } catch (e: Exception) {
            Log.e(TAG, "loadReplies failed", e)
        }
    }

    private fun addItem(trigger: String, reply: String) {
        try {
            val inflater = LayoutInflater.from(this)
            val item = inflater.inflate(R.layout.item_reply, llReplies, false)
            item.findViewById<TextView>(R.id.tv_trigger)?.text = ": $trigger"
            item.findViewById<TextView>(R.id.tv_reply)?.text = ": $reply"
            item.findViewById<ImageButton>(R.id.btn_delete)?.setOnClickListener {
                Prefs.removeReply(trigger)
                llReplies?.removeView(item)
            }
            llReplies?.addView(item)
        } catch (e: Exception) {
            Log.e(TAG, "addItem failed", e)
        }
    }
}
