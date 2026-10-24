plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firebase aktif otomatis jika file app/google-services.json ada.
// Jika belum ada, aplikasi tetap bisa di-build dan dijalankan.
val hasFirebase = file("google-services.json").exists()
if (hasFirebase) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.dapursappa.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dapursappa.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 19
        versionName = "0.4.12"
    }

    // Kunci tanda tangan tetap supaya update bisa dipasang di atas versi lama,
    // baik dibuild dari Android Studio maupun dari GitHub Actions.
    signingConfigs {
        create("sappa") {
            storeFile = file("dapursappa.jks")
            storePassword = "dapursappa"
            keyAlias = "dapursappa"
            keyPassword = "dapursappa"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sappa")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sappa")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    if (hasFirebase) {
        implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
        implementation("com.google.firebase:firebase-firestore")
        implementation("com.google.firebase:firebase-auth")
    }
}
