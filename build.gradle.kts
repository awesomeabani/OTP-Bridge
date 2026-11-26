plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.otpbridge"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.otpbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-auth-api-phone:18.2.0")
    implementation("androidx.core:core-ktx:1.15.0")
}
