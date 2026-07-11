plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.noor.prism"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.noor.prism"
        minSdk = 21
        targetSdk = 34
        versionCode = 11
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // CRITICAL TV FIX: Bypasses strict production signature locks on your TV Stick
            isDebuggable = true 
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
