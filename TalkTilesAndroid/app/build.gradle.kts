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
val hasReleaseKey = keystoreFile.exists() && !keystorePass.isNullOrEmpty()

android {
    namespace = "com.talktiles.tablet"
    compileSdk = 36

    defaultConfig {
        // The Play listing is tied to this id forever; it was chosen before
        // the first upload. The code package stays com.talktiles.tablet.
        applicationId = "com.talktiles.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "2.0-preview"
        vectorDrawables { useSupportLibrary = true }
    }

    buildToolsVersion = "36.1.0"

    signingConfigs {
        if (hasReleaseKey) {
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
            // R8 shrinks the code (Compose + icons-extended are the bulk of
            // the dex); the assets are what make the bundle big and are left
            // alone. Rules for kotlinx.serialization are in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Never fall back to the debug key: a release signed with anything
            // but the Talk Tiles key cannot install over the book on the tablet.
            // Without the key the release tasks fail (see the task-graph check).
            signingConfig = signingConfigs.findByName("talktiles")
        }
    }

    bundle {
        // One bundle, no per-language/density splits worth making: the app
        // has one language and almost no drawables.
        language { enableSplit = false }
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

    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.4" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // WebP/MP3/PNG are already compressed; storing them uncompressed lets
        // them be read straight out of the APK without inflating.
    }
    androidResources {
        noCompress += listOf("webp", "mp3", "png", "json")
    }

    testOptions {
        // Robolectric hosts the Compose semantics tests on the JVM; the app's
        // resources (theme, file provider paths) are needed for that.
        unitTests.isIncludeAndroidResources = true
        // android.util.Log in plain JVM tests returns 0 instead of throwing.
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.jvmArgs("-Xmx3g")
            // Order and outcome of every test in the log, so a run can be audited.
            it.testLogging { events("started", "passed", "failed", "skipped") }
        }
    }
}

// Explicit failure beats a quietly unsigned or debug-signed release.
gradle.taskGraph.whenReady {
    val releaseTasks = setOf("packageRelease", "signReleaseBundle", "bundleRelease", "assembleRelease")
    if (!hasReleaseKey && allTasks.any { it.project == project && it.name in releaseTasks }) {
        throw GradleException("Release builds must be signed with the Talk Tiles key. Point KS at the keystore and set KS_PASS (or the .keystore-pass file beside it).")
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
    // Talk Tiles Pro, the one-time in-app purchase. Play requires Billing 8+ for updates from Aug 2026.
    implementation("com.android.billingclient:billing:8.0.0")

    // Tests: plain JUnit for the models, navigation, sentence and speech
    // state machines; Robolectric + Compose UI test for semantics and sheets.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
