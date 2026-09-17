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
        versionCode = 5
        versionName = "1.2.2"

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nur 64-Bit-ABIs für Play-16-KB-Anforderungen und Emulatoren.
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

    ndkVersion = "28.1.10738933"

    packaging {
        jniLibs {
            // Unkomprimierte .so mit 16-KB-ZIP-Alignment.
            useLegacyPackaging = false
            // Compose-graphics-path: RELRO nicht 16-KB-fähig; ab API 34 nutzt Compose die Plattform-API.
            excludes += listOf("**/libandroidx.graphics.path.so")
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

    // QR-Scan über Play Services (keine nativen .so in der App-APK).
    implementation(libs.play.services.code.scanner)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
