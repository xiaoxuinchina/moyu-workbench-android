plugins { id("com.android.application") }

android {
    namespace = "com.moyu.workbench"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moyu.workbench"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val ksPath = System.getenv("MOYU_KEYSTORE_PATH")
    if (!ksPath.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = file(ksPath)
                storeType = "PKCS12"
                storePassword = System.getenv("MOYU_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MOYU_KEY_ALIAS")
                keyPassword = System.getenv("MOYU_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingConfigs.findByName("release") != null) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
