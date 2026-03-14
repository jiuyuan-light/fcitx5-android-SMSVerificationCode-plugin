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

    fun normalizeVersionName(input: String): String {
        val v = if (input.startsWith("v")) input.substring(1) else input
        return if (Regex("^\\d+\\.\\d+\\.\\d+$").matches(v)) v else Versions.baseVersionName
    }

    fun versionCodeOf(versionName: String): Int {
        val parts = versionName.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 1_000_000 + minor * 1_000 + patch
    }

    val appVersionName = normalizeVersionName(System.getenv("PLUGIN_VERSION") ?: Versions.baseVersionName)
    val appVersionCode = versionCodeOf(appVersionName)

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.sms"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = appVersionCode
        versionName = appVersionName
        manifestPlaceholders["enableNotificationListener"] = "true"
        
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
            val storeFilePath =
                System.getenv("SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() }
                    ?: (project.findProperty("signing.storeFile") as? String)?.takeIf { it.isNotBlank() }
                    ?: localProps.getProperty("signing.storeFile")?.takeIf { it.isNotBlank() }
                    ?: "D:/code/worker/tmp/Fcitx5-Android-SMS-Plugin.p12"
            storeFile = file(storeFilePath)
            storeType = "PKCS12"

            val sp =
                System.getenv("SIGNING_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: (project.findProperty("signing.storePassword") as? String)?.takeIf { it.isNotBlank() }
                    ?: localProps.getProperty("signing.storePassword")?.takeIf { it.isNotBlank() }
                    ?: error("Missing signing.storePassword (set in local.properties or env SIGNING_STORE_PASSWORD)")
            storePassword = sp

            val alias =
                System.getenv("SIGNING_KEY_ALIAS")?.takeIf { it.isNotBlank() }
                    ?: (project.findProperty("signing.keyAlias") as? String)?.takeIf { it.isNotBlank() }
                    ?: localProps.getProperty("signing.keyAlias")?.takeIf { it.isNotBlank() }
                    ?: "fcitx5-android-sms-plugin"
            keyAlias = alias

            val kp =
                System.getenv("SIGNING_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: (project.findProperty("signing.keyPassword") as? String)?.takeIf { it.isNotBlank() }
                    ?: localProps.getProperty("signing.keyPassword")?.takeIf { it.isNotBlank() }
                    ?: sp
            keyPassword = kp
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "@string/app_name_release")
            buildConfigField("boolean", "ENABLE_NOTIFICATION_LISTENER", "true")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
            buildConfigField("boolean", "ENABLE_NOTIFICATION_LISTENER", "true")
            manifestPlaceholders["enableNotificationListener"] = "true"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
}
