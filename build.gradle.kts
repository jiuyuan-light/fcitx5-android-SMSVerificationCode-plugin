import java.util.Properties

plugins {
    id("com.android.application") version "8.7.0"
    id("org.jetbrains.kotlin.android") version "1.8.22"
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.sms"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.sms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1012007
        versionName = System.getenv("PLUGIN_VERSION") ?: "0.1.2"
        setProperty("archivesBaseName", "fcitx5-sms-plugin-$versionName")

        val keywordFile = rootProject.file("default_keywords.txt")
        val keywordLines = if (keywordFile.exists()) {
            keywordFile.readLines(Charsets.UTF_8)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
        } else {
            listOf("验证码", "校验码", "动态码", "确认码", "取件码", "提货码", "一次性", "口令")
        }
        val keywordJoined = keywordLines.joinToString(", ")
        fun escapeRes(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
        resValue("string", "default_keywords", escapeRes(keywordJoined))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    signingConfigs {
        create("release") {
            val props = Properties()
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
            
            storeFile = (System.getenv("SIGNING_STORE_FILE") ?: props.getProperty("signing.storeFile"))?.let { file(it) }
            storeType = "PKCS12"
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: props.getProperty("signing.storePassword") ?: ""
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: props.getProperty("signing.keyAlias") ?: "fcitx5-android-sms-plugin"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: props.getProperty("signing.keyPassword") ?: storePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
