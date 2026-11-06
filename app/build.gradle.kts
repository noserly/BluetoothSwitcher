plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.btalarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.btalarm"
        minSdk = 26
        // ВАЖНО: с targetSdk 33+ Android запрещает приложениям вызывать BluetoothAdapter.disable()
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
