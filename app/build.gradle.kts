plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // CRITICAL: This dictates how resources and R layout folders are mapped!
    namespace = "com.noor.prism" 
    compileSdk = 34

    defaultConfig {
        // CRITICAL: This tells the TV OS exactly who owns this application package!
        applicationId = "com.noor.prism"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
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
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Core Data Frameworks
    implementation("com.google.code.gson:gson:2.10.1")
    // THIS STOPS THE OKHTTP3 UNRESOLVED ERRORS IN MAINACTIVITY
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
