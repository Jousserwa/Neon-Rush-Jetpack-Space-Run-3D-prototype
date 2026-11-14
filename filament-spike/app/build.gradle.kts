plugins {
    id("com.android.application")
}

android {
    namespace = "com.neonrush.filamentspike"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neonrush.filamentspike"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("com.google.android.filament:filament-android:1.80.1")
    implementation("com.google.android.filament:gltfio-android:1.80.1")
    implementation("com.google.android.filament:filament-utils-android:1.80.1")
}
