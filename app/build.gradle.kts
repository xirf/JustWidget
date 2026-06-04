import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 has built-in Kotlin support, so org.jetbrains.kotlin.android is NOT
    // applied here. Only the Compose compiler plugin is needed for Compose/Glance.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "id.andka.justwidget"

    val signingProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingProperties.getProperty("signing.storeFile") ?: System.getenv("SIGNING_STORE_FILE")
            val keystoreStorePassword = signingProperties.getProperty("signing.storePassword") ?: System.getenv("SIGNING_STORE_PASSWORD")
            val keystoreKeyAlias = signingProperties.getProperty("signing.keyAlias") ?: System.getenv("SIGNING_KEY_ALIAS")
            val keystoreKeyPassword = signingProperties.getProperty("signing.keyPassword") ?: System.getenv("SIGNING_KEY_PASSWORD")

            val storeFilePath = keystorePath?.let { rootProject.file(it) }
            val isPlaceholder = keystoreStorePassword == "YOUR_KEYSTORE_PASSWORD" || keystoreKeyPassword == "YOUR_KEY_PASSWORD"
            if (storeFilePath != null && storeFilePath.exists() && !isPlaceholder && keystoreStorePassword != null && keystoreKeyAlias != null && keystoreKeyPassword != null) {
                storeFile = storeFilePath
                storePassword = keystoreStorePassword
                keyAlias = keystoreKeyAlias
                keyPassword = keystoreKeyPassword
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "id.andka.justwidget"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
    }

    // With built-in Kotlin the Kotlin DSL lives inside the android block.
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Glance app widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Compose (settings Activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Location + background refresh
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
