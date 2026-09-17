plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.msjones.android.alarmapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.msjones.android.alarmapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.2.1"

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nur 64-Bit-ABIs: ML-Kit-Barhopper ist auf 32-Bit (armeabi-v7a/x86) nur 4-KB-aligniert
        // und löst sonst die Systemwarnung „isn't 16 KB compatible“ aus.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    // NDK r28+ erzeugt standardmäßig 16-KB-alignte native Bibliotheken.
    ndkVersion = "28.1.10738933"

    packaging {
        jniLibs {
            // Unkomprimierte .so mit 16-KB-ZIP-Alignment (Play-Store-Anforderung).
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.hivemq.mqtt.client)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)

    // ML Kit Barcode Scanning (bundled, 16-KB-kompatibel ab 17.3.0)
    implementation(libs.mlkit.barcode.scanning)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
