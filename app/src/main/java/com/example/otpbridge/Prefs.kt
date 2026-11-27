package com.example.otpbridge

import android.content.Context
import java.text.DateFormat
import java.util.Date

data class Config(
    val url: String,
    val token: String,
    val filter: String,
    val enabled: Boolean
) {
    fun isComplete() = RelayClient.isValidUrl(url) && RelayClient.isValidToken(token)
}

object Prefs {
    private const val FILE = "otp_bridge"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(c: Context): Config {
        val p = sp(c)
        return Config(
            url = p.getString("url", "").orEmpty(),
            token = p.getString("token", "").orEmpty(),
            filter = p.getString("filter", "IVAC").orEmpty(),
            enabled = p.getBoolean("enabled", true)
        )
    }

    fun save(c: Context, cfg: Config) {
        sp(c).edit()
            .putString("url", cfg.url)
            .putString("token", cfg.token)
            .putString("filter", cfg.filter)
            .putBoolean("enabled", cfg.enabled)
            .apply()
    }

    /** Last thing the receiver did, shown on the main screen. Never stores SMS text or the OTP. */
    fun setStatus(c: Context, text: String) {
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        sp(c).edit().putString("last_status", "$time  $text").apply()
    }

    fun lastStatus(c: Context): String =
        sp(c).getString("last_status", null) ?: "এখনো কোনো SMS আসেনি।"
}
