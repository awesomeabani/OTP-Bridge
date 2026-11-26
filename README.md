# OTP Bridge — Android source + Chrome Extension v21.2.1

## বর্তমান Cloudflare সেটআপের সঙ্গে মিল
আপনার Cloudflare Worker-এর Health Check সফল হলে Worker URL সচল। এই সংস্করণটি নিচের সেটআপের সঙ্গে মেলে:
- KV binding variable: `OTP_BRIDGE_KV`
- Worker Secret variable: `BRIDGE_TOKEN`
- Android sends OTP to `POST /send`
- Chrome Extension receives OTP from `GET /receive`
- Health check: `GET /health`
- OTP TTL: 120 seconds; receive হলে KV entry delete করা হয়।

এই Project-এর `worker/worker.js` হলো একই interface-এর source copy। Cloudflare-এ ইতিমধ্যে Worker deploy করা থাকলে আবার deploy করার প্রয়োজন নেই।

## ব্যবহার
1. Android app-এ HTTPS Cloudflare Worker URL এবং একই pairing token দিন।
2. **Listen for OTP SMS** চাপুন। SMS এলে Google SMS User Consent prompt দেখাবে।
3. Google consent দিলে app SMS থেকে ৬ সংখ্যার কোড অথবা one-two-three-four-five-six-এর মতো ছয়টি ইংরেজি সংখ্যা-শব্দ শনাক্ত করে অঙ্কে রূপান্তর করবে। একাধিক সম্ভাব্য কোড থাকলে কিছু পাঠাবে না।
4. **Forward OTP to my Extension** চাপলে OTP relay-তে যাবে।
5. Windows Chrome-এ IVAC Extension popup খুলে রাখুন; একই Relay URL ও token Save করুন। Popup খোলা থাকলে এটি `/receive` poll করে।
6. OTP ছয়টি Extension box-এ এলে **Fill OTP** চাপুন। Verify/Submit আপনাকে নিজে চাপতে হবে।

## Chrome Extension install/update
1. ZIP extract করুন।
2. Chrome-এ `chrome://extensions` খুলুন এবং Developer mode চালু করুন।
3. আগে একই IVAC extension-এর পুরোনো folder Load unpacked থাকলে সেটি Remove করুন (শুধু extension entry; profile data browser storage-এ থাকতে পারে), তারপর **Load unpacked** চাপুন।
4. Extract করা `IVAC-Extension` folder নির্বাচন করুন।
5. IVAC page refresh করুন।

## Android app build
এই ZIP-এ Android Studio project source আছে, APK নয়। Android Studio-তে `OTP-Bridge` folder খুলে Gradle sync করুন; Android SDK 35 প্রয়োজন। তারপর Build > Build APK(s)। Android SDK/Gradle ছাড়া এখানে APK compile করা হয়নি।

## Android Studio ছাড়া APK build (GitHub Actions)
1. GitHub-এ একটি private repository তৈরি করুন।
2. ZIP extract করে `OTP-Bridge` folder-এর ভেতরের সব ফাইল repository-তে upload/push করুন।
3. **Actions** tab খুলে **Build OTP Bridge APK** workflow-এ **Run workflow** চাপুন।
4. সফল হলে workflow artifact `OTP-Bridge-debug-apk` থেকে `app-debug.apk` নিন।

## Security and limitations
- App broad `READ_SMS` permission চায় না; Google SMS User Consent prompt ব্যবহার করে।
- OTP স্বয়ংক্রিয়ভাবে পাঠানো হয় না; Google SMS consent দেওয়ার পর ব্যবহারকারীকে কোড দেখে Android-এর **Forward OTP** চাপতে হয়।
- Token কমপক্ষে ৩২ অক্ষরের random string হওয়া উচিত এবং কখনো শেয়ার করা উচিত নয়।
- Chrome popup বন্ধ থাকলে receiver polling বন্ধ থাকে। OTP আসার সময় popup খোলা রাখুন।
- Cloudflare KV-এর get/delete atomic নয়; একই token-এ একাধিক receiver একসঙ্গে poll করবেন না।
- CAPTCHA, IVAC authentication বা Verify/Submit control bypass করা হয় না।
