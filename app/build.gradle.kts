plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val ciVersionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val signingStorePath = System.getenv("NR_KEYSTORE_PATH")
val signingStorePassword = System.getenv("NR_KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("NR_KEY_ALIAS")
val signingKeyPassword = System.getenv("NR_KEY_PASSWORD")
val hasPersistentSigning = !signingStorePath.isNullOrBlank()
    && !signingStorePassword.isNullOrBlank()
    && !signingKeyAlias.isNullOrBlank()
    && !signingKeyPassword.isNullOrBlank()

android {
    namespace = "com.neonrush.prototype3d"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neonrush.prototype3d"
        minSdk = 24
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = "0.1.0"
    }

    if (hasPersistentSigning) {
        signingConfigs {
            create("prototype") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasPersistentSigning) {
                signingConfig = signingConfigs.getByName("prototype")
            }
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
    implementation(libs.filament.android)
    implementation(libs.filament.utils.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
