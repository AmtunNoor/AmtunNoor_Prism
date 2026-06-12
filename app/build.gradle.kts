buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = true // Keeps development launch permissions active on your TV stick
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
