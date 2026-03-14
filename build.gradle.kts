import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.8.22"
}

object Versions {
    val javaVersion = JavaVersion.VERSION_1_8
    const val compileSdk = 35
    const val minSdk = 24
    const val targetSdk = 35
    const val buildTools = "35.0.0"
    const val baseVersionName = "0.1.2"
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.sms"
    compileSdk = Versions.compileSdk
    buildToolsVersion = Versions.buildTools

    fun loadLocalProperties(): Properties {
        val props = Properties()
        val f = rootProject.file("local.properties")
        if (f.exists()) {
            FileInputStream(f).use { props.load(it) }
        }
        return props
    }

    val appVersionName = System.getenv("PLUGIN_VERSION") ?: Versions.baseVersionName
    val appVersionCode = 1012003

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.sms"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = appVersionCode
        versionName = appVersionName
        
        setProperty("archivesBaseName", "fcitx5-sms-plugin-$appVersionName")
    }

    buildFeatures {
        resValues = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = Versions.javaVersion
        targetCompatibility = Versions.javaVersion
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    signingConfigs {
        create("release") {
            val localProps = loadLocalProperties()
            val storeFilePath = System.getenv("SIGNING_STORE_FILE")
                ?: localProps.getProperty("signing.storeFile")
                ?: "D:/code/worker/tmp/Fcitx5-Android-SMS-Plugin.p12"
            storeFile = file(storeFilePath)
            storeType = "PKCS12"

            val sp = System.getenv("SIGNING_STORE_PASSWORD")
                ?: localProps.getProperty("signing.storePassword")
                ?: error("Missing signing.storePassword")
            storePassword = sp

            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: localProps.getProperty("signing.keyAlias")
                ?: "fcitx5-android-sms-plugin"

            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: localProps.getProperty("signing.keyPassword")
                ?: sp
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "@string/app_name_release")
            buildConfigField("boolean", "ENABLE_NOTIFICATION_LISTENER", "true")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
            buildConfigField("boolean", "ENABLE_NOTIFICATION_LISTENER", "true")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
}
