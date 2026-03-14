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
        val versionFile = rootProject.file("version.properties")
        val versionProps = Properties().apply {
            if (versionFile.exists()) {
                versionFile.inputStream().use { load(it) }
            }
        }
        val envVersionName = System.getenv("PLUGIN_VERSION")
        val envVersionCode = System.getenv("PLUGIN_VERSION_CODE")
        val fileVersionName = versionProps.getProperty("versionName")
        val fileVersionCode = versionProps.getProperty("versionCode")
        val fallbackVersionName = "0.1.2"
        val fallbackVersionCode = 1012007

        versionName = envVersionName ?: fileVersionName ?: fallbackVersionName
        versionCode = envVersionCode?.toIntOrNull()
            ?: fileVersionCode?.toIntOrNull()
            ?: fallbackVersionCode
        setProperty("archivesBaseName", "fcitx5-sms-plugin-$versionName")

        val keywordFile = rootProject.file("default_keywords.txt")
        val keywordLines = if (keywordFile.exists()) {
            keywordFile.readLines(Charsets.UTF_8)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
        } else {
            listOf("一次性", "口令", "动态码", "取件码", "提货码", "校验码", "确认码", "验证码")
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
    testImplementation("junit:junit:4.13.2")
}
