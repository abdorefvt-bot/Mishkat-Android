plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.abdelrahman.mishkat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abdelrahman.mishkat"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        buildConfigField("String", "WEB_URL", "\"https://mishkatt.framer.website\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
}
