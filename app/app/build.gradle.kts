plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.dkajan.notifixer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dkajan.notifixer"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations.addAll(listOf("en"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // COMPOSE
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // SHIZUKU
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Core Splashscreen
    implementation("androidx.core:core-splashscreen:1.0.1")
}