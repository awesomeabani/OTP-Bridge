package com.example.otpbridge

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RelayClient {
    private val tokenRegex = Regex("[A-Za-z0-9_-]{32,128}")

    fun isValidToken(token: String) = tokenRegex.matches(token)

    fun isValidUrl(base: String): Boolean = try {
        val u = URL(base)
        u.protocol == "https" && u.host.endsWith(".workers.dev") &&
            u.host.length > ".workers.dev".length && u.userInfo == null
    } catch (_: Exception) { false }

    /** Authenticated connectivity check. Returns (ok, message). */
    fun checkStatus(base: String, token: String): Pair<Boolean, String> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(base.trimEnd('/') + "/status").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "Bearer $token")
            }
            when (val code = conn.responseCode) {
                200 -> true to "সংযোগ ঠিক আছে ✔"
                401 -> false to "Token মেলেনি (HTTP 401)। Extension ও Worker-এর token এক হতে হবে।"
                else -> false to "ব্যর্থ (HTTP $code)"
            }
        } catch (e: Exception) {
            false to "সংযোগ সমস্যা: ${e.localizedMessage}"
        } finally { conn?.disconnect() }
    }

    /**
     * Sends the OTP, retrying quickly while the deadline allows. A broadcast
     * receiver only has a few seconds, so the whole call is capped at ~8 s.
     */
    fun sendOtp(base: String, token: String, otp: String): Pair<Boolean, String> {
        val deadline = System.currentTimeMillis() + 8000
        var last = "অজানা ত্রুটি"
        var attempt = 0
        while (attempt < 3 && System.currentTimeMillis() < deadline - 1500) {
            attempt++
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(base.trimEnd('/') + "/send").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Cache-Control", "no-store")
                }
                conn.outputStream.use {
                    it.write(JSONObject().put("otp", otp).toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                if (code in 200..299) return true to "OTP Extension-এর জন্য পাঠানো হয়েছে ✔"
                last = "HTTP $code"
                if (code == 401 || code == 400) break // retrying cannot help
            } catch (e: Exception) {
                last = e.localizedMessage ?: "সংযোগ সমস্যা"
            } finally { conn?.disconnect() }
            try { Thread.sleep(500) } catch (_: InterruptedException) { break }
        }
        return false to "পাঠানো যায়নি ($last)"
    }
}
