package com.example.otpbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Runs for every incoming SMS (even when the app is closed). It only forwards
 * messages that match the filter and contain exactly one six-digit OTP.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val cfg = Prefs.load(context)
        if (!cfg.enabled || !cfg.isComplete()) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return
        val sender = parts[0].displayOriginatingAddress.orEmpty()
        val body = parts.joinToString("") { it.displayMessageBody.orEmpty() }

        val filter = cfg.filter.trim()
        if (filter.isNotEmpty() &&
            !sender.contains(filter, ignoreCase = true) &&
            !body.contains(filter, ignoreCase = true)
        ) {
            Prefs.setStatus(context, "উপেক্ষা করা হয়েছে: filter “$filter” মেলেনি (প্রেরক: $sender)")
            return
        }

        val candidates = OtpExtractor.extract(body)
        if (candidates.size != 1) {
            Prefs.setStatus(
                context,
                if (candidates.isEmpty()) "উপেক্ষা: SMS-এ ৬ সংখ্যার OTP নেই (প্রেরক: $sender)"
                else "উপেক্ষা: একাধিক সম্ভাব্য OTP, নিরাপত্তার জন্য কিছু পাঠানো হয়নি"
            )
            return
        }

        val otp = candidates.first()
        val pending = goAsync()
        Thread {
            try {
                val (_, message) = RelayClient.sendOtp(cfg.url, cfg.token, otp)
                Prefs.setStatus(context, message)
            } catch (e: Exception) {
                Prefs.setStatus(context, "ত্রুটি: ${e.localizedMessage}")
            } finally {
                pending.finish()
            }
        }.start()
    }
}
