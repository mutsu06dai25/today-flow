plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub のビルド番号をバージョンにする（上書きインストールできるように毎回増える）
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "jp.todayflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.todayflow"
        minSdk = 29
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // 毎回同じ鍵で署名する。鍵が変わると更新時にアンインストールが必要になり、データが消えるため。
    signingConfigs {
        create("fixed") {
            storeFile = file("release.jks")
            storePassword = "todayflow"
            keyAlias = "todayflow"
            keyPassword = "todayflow"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
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
