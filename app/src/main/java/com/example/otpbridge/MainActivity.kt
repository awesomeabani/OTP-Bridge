package com.example.otpbridge

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.security.SecureRandom
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var url: EditText
    private lateinit var token: EditText
    private lateinit var filter: EditText
    private lateinit var enabled: Switch
    private lateinit var permission: TextView
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun topInset(i: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= 30) i.getInsets(WindowInsets.Type.systemBars()).top else i.systemWindowInsetTop

    @Suppress("DEPRECATION")
    private fun bottomInset(i: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= 30) i.getInsets(WindowInsets.Type.systemBars()).bottom else i.systemWindowInsetBottom

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFFF7FAF9.toInt())
            addView(col)
            setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(0, topInset(insets), 0, bottomInset(insets))
                insets
            }
        }

        fun label(text: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(0xFF16352E.toInt())
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(2))
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text; isAllCaps = false; setOnClickListener { action() }
        }
        fun add(v: android.view.View) = col.addView(
            v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        add(label("OTP Bridge", 26f, true))
        add(label("ফোনে IVAC-এর SMS এলে OTP নিজে থেকে কম্পিউটারের Extension-এ চলে যাবে।"))

        add(label("১. Pairing", 17f, true))
        add(button("Extension-এর Pairing code Paste করুন") { pastePairingCode() })
        url = EditText(this).apply { hint = "Relay URL (https://….workers.dev)"; setSingleLine(true) }
        token = EditText(this).apply { hint = "Pairing token"; setSingleLine(true) }
        add(url); add(token)
        add(label("SMS filter (প্রেরকের নাম বা SMS-এর লেখায় থাকতে হবে; ফাঁকা = সব SMS)"))
        filter = EditText(this).apply { setSingleLine(true) }
        add(filter)
        enabled = Switch(this).apply { text = "OTP পাঠানো চালু"; textSize = 16f }
        add(enabled)
        add(button("Save") { save(); setStatus("Saved।") })
        add(button("Test connection") { testConnection() })

        add(label("২. SMS অনুমতি", 17f, true))
        permission = label("")
        add(permission)
        add(button("SMS অনুমতি দিন") {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQ_SMS)
        })

        add(label("৩. সর্বশেষ অবস্থা", 17f, true))
        status = label("", 14f)
        add(status)
        add(label(
            "গোপনীয়তা: শুধু filter-এ মেলা এবং ঠিক একটি ৬ সংখ্যার OTP থাকা SMS-এর OTP পাঠানো হয়। " +
                "SMS-এর লেখা কোথাও জমা বা পাঠানো হয় না। OTP ১২০ সেকেন্ড বা একবার নেওয়ার পর মুছে যায়।", 12f
        ))
        setContentView(scroll)
        load()
    }

    override fun onResume() {
        super.onResume()
        refreshPermission()
        status.text = Prefs.lastStatus(this)
    }

    private fun setStatus(text: String) { status.text = text }

    private fun load() {
        val c = Prefs.load(this)
        url.setText(c.url); token.setText(c.token); filter.setText(c.filter); enabled.isChecked = c.enabled
    }

    private fun currentConfig() = Config(
        url = url.text.toString().trim().trimEnd('/'),
        token = token.text.toString().trim(),
        filter = filter.text.toString().trim(),
        enabled = enabled.isChecked
    )

    private fun save(): Boolean {
        val cfg = currentConfig()
        if (!RelayClient.isValidUrl(cfg.url) || !RelayClient.isValidToken(cfg.token)) {
            setStatus("সঠিক HTTPS workers.dev URL এবং ৩২–১২৮ অক্ষরের token দিন।")
            return false
        }
        Prefs.save(this, cfg)
        return true
    }

    private fun pastePairingCode() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty().trim()
        val parts = text.split("|")
        if (parts.size != 3 || parts[0] != "OTPB1") {
            setStatus("Clipboard-এ সঠিক Pairing code নেই। Extension থেকে কপি করুন।")
            return
        }
        url.setText(parts[1].trim()); token.setText(parts[2].trim())
        if (save()) setStatus("Pairing code বসানো ও Save হয়েছে। এবার Test connection চাপুন।")
    }

    private fun testConnection() {
        if (!save()) return
        val cfg = currentConfig()
        setStatus("পরীক্ষা করা হচ্ছে…")
        executor.execute {
            val (_, message) = RelayClient.checkStatus(cfg.url, cfg.token)
            runOnUiThread { setStatus(message) }
        }
    }

    private fun hasSmsPermission() =
        checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun refreshPermission() {
        permission.text = if (hasSmsPermission()) "✔ SMS অনুমতি দেওয়া আছে।"
        else "✘ অনুমতি নেই। Android 13+ হলে যদি অনুমতি ধূসর থাকে: Settings → Apps → OTP Bridge → ⋮ → " +
            "“Allow restricted settings”, তারপর আবার চেষ্টা করুন।"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermission()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    companion object { private const val REQ_SMS = 701 }
}
