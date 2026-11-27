# OTP Bridge v2: ফোনের OTP → IVAC Extension (স্বয়ংক্রিয়)

```
ফোনে SMS ──► Android app ──► Cloudflare Worker (relay) ──► Extension ──► Verify OTP বক্সে বসে
                                (১২০ সেকেন্ড/একবার)                         (Verify আপনি চাপবেন)
```

তিনটি অংশ: `relay-worker/`, `android/`, `extension/` (IVAC v21.3.0)।

## ধাপ ১: Relay (একবার, কম্পিউটারে)
1. Node.js ইনস্টল থাকতে হবে। বিনামূল্যের Cloudflare account লাগবে।
2. `cd relay-worker`
3. `npx wrangler login`
4. `npx wrangler deploy`  → শেষে একটি URL দেখাবে, যেমন `https://otp-bridge-relay.NAME.workers.dev`
5. পরের ধাপে Extension থেকে token তৈরি করে এখানে বসাবেন: `npx wrangler secret put BRIDGE_TOKEN`

## ধাপ ২: Extension
1. `chrome://extensions` → পুরোনোটি Remove → Developer mode → Load unpacked → `extension/` ফোল্ডার।
2. Extension popup-এর নিচে **OTP Bridge** বক্সে:
   - **Generate token** চাপুন, token কপি করে ধাপ ১.৫-এ Worker-এ বসান।
   - Relay URL বসিয়ে **Save** চাপুন, তারপর **Test connection** (✔ আসতে হবে)।
   - নিচের **Pairing code** কপি করুন।

## ধাপ ৩: Android app
1. `android/` ফোল্ডার GitHub-এ আপলোড করে Actions → *Build OTP Bridge APK* চালান (বা Android Studio-তে খুলে Build)। `app-debug.apk` ফোনে ইনস্টল করুন।
2. App খুলে **Pairing code Paste করুন** (code আগে ফোনে কপি/পাঠিয়ে নিন), তারপর **Test connection**।
3. **SMS অনুমতি দিন**।
   Android 13+ এ sideload করা app-এ অনুমতি ধূসর থাকলে: Settings → Apps → OTP Bridge → ⋮ → *Allow restricted settings*।
4. **SMS filter** ঘরে IVAC-এর SMS-এর প্রেরকের নাম বা SMS-এ থাকা একটি শব্দ দিন (ডিফল্ট `IVAC`)।
   না মিললে app-এর "সর্বশেষ অবস্থা"-য় প্রেরকের নাম দেখাবে, সেটা filter-এ দিন।

## ব্যবহার
Verify OTP পেজ খোলা রাখুন। SMS এলে ১–৩ সেকেন্ডে OTP বক্সে বসবে ও নিচে সবুজ বার্তা আসবে। **Verify OTP আপনি নিজে চাপবেন।**

## নিরাপত্তা
- Token, URL ও Pairing code গোপন রাখুন। যে জানে সে আপনার relay-তে OTP পাঠাতে/নিতে পারবে। সন্দেহ হলে নতুন token তৈরি করে Worker, Extension ও app-এ বদলান।
- App শুধু filter-এ মেলা এবং ঠিক একটি ৬ সংখ্যার OTP থাকা SMS পাঠায়; SMS-এর লেখা কোথাও জমা হয় না।
- Relay-তে OTP ১২০ সেকেন্ড বা একবার নেওয়ার পর মুছে যায়।

## সীমাবদ্ধতা
- Android অংশ এই পরিবেশে compile করা যায়নি (Android SDK নেই)। Worker লজিক পরীক্ষিত, extension JS সিনট্যাক্স-পরীক্ষিত। প্রথমবার build-এ ত্রুটি এলে আমাকে দেখান।
- Live IVAC পেজে পরীক্ষা হয়নি।
