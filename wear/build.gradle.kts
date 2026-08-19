plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.norwegian4x4"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.norwegian4x4"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose for Wear OS
    implementation("androidx.compose.ui:ui:1.7.2")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")

    // Health Services (heart rate, GPS, distance on Wear OS)
    implementation("androidx.health:health-services-client:1.1.0-alpha05")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")

    // Lifecycle + service helpers
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Data Layer: send workouts to the phone companion app
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
}
