import java.util.Properties

plugins {
    id("com.android.application") version "8.7.0"
    id("org.jetbrains.kotlin.android") version "1.8.22"
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.sms"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.sms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1012006
        versionName = System.getenv("PLUGIN_VERSION") ?: "0.1.2"
        setProperty("archivesBaseName", "fcitx5-sms-plugin-$versionName")
    }

    buildFeatures {
        resValues = true
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
            resValue("string", "app_name", "@string/app_name_release")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }
}

val verifyFcitxPluginMetadata = tasks.register("verifyFcitxPluginMetadata") {
    doLast {
        if (project.findProperty("skipPluginMetadataCheck") != null) return@doLast

        fun requireOk(ok: Boolean, message: String) {
            if (!ok) throw GradleException(message)
        }

        val pluginXml = file("src/main/res/xml/plugin.xml").readText()
        requireOk(pluginXml.contains("<domain>fcitx5-sms</domain>"), "plugin.xml must keep domain=fcitx5-sms")

        val descriptorJson = file("src/main/assets/descriptor.json").readText()
        requireOk(Regex("\"sha256\"\\s*:\\s*\"0{64}\"").containsMatchIn(descriptorJson), "descriptor.json sha256 must be 64 zeros")
        requireOk(Regex("\"files\"\\s*:\\s*\\{\\s*\\}").containsMatchIn(descriptorJson), "descriptor.json files must be empty object")

        val stringsXmlFile = file("src/main/res/values/strings.xml")
        requireOk(stringsXmlFile.exists(), "strings.xml must exist")
        val stringsXml = stringsXmlFile.readText()
        requireOk(stringsXml.contains("name=\"app_name\""), "strings.xml must define app_name")

        val manifest = file("src/main/AndroidManifest.xml").readText()
        requireOk(manifest.contains("android:label=\"@string/app_name\""), "AndroidManifest.xml must use @string/app_name label")
        requireOk(manifest.contains("android.permission.READ_SMS"), "AndroidManifest.xml must request READ_SMS")
    }
}

tasks.matching { it.name == "preBuild" || it.name == "preReleaseBuild" || it.name == "preDebugBuild" }.configureEach {
    dependsOn(verifyFcitxPluginMetadata)
}
