plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.noor.prism"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.noor.prism"
        minSdk = 21
        targetSdk = 35
        versionCode = 16
        versionName = "3.0.0-rc1"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // Stable installable RC. Keep shrinking off until device validation is complete.
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
