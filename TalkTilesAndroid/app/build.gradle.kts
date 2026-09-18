plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// The signing key is the Talk Tiles key that already signs the web-based
// Android build (aac-board/aac.keystore). Android identifies an app by its
// signature, so this is what lets a newer build install over an older one
// and keep the board. The password is deliberately not in this file: it is
// read from KS_PASS in the environment, else the gitignored .keystore-pass
// next to the keystore.
val keystoreFile = file(System.getenv("KS") ?: "/home/mike/aac-board/aac.keystore")
val keystorePass: String? = System.getenv("KS_PASS")
    ?: keystoreFile.parentFile.resolve(".keystore-pass").takeIf { it.exists() }?.readText()?.trim()

android {
    namespace = "com.talktiles.tablet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.talktiles.tablet"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        vectorDrawables { useSupportLibrary = true }
    }

    buildToolsVersion = "34.0.0"

    signingConfigs {
        if (keystoreFile.exists() && keystorePass != null) {
            create("talktiles") {
                storeFile = keystoreFile
                storePassword = keystorePass
                keyAlias = "aacboard"
                keyPassword = keystorePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("talktiles") ?: signingConfigs.getByName("debug")
        }
    }

    // The pictures and Bella's clips are the SwiftUI app's folders: the
    // entries in src/main/assets are symlinks into ../AACTextTilesSwiftUI, so
    // both apps ship exactly the same files and Tools/sync-native-assets.py
    // refreshes both at once.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.4" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // WebP/MP3/PNG are already compressed; storing them uncompressed lets
        // them be read straight out of the APK without inflating.
    }
    androidResources {
        noCompress += listOf("webp", "mp3", "png", "json")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
