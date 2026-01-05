plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

// --- PINDAHKAN KE SINI (DI ATAS) ---
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
// -----------------------------------

android {
    namespace = "com.example.poshon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.poshon"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Library Printer (Pastikan versi 3.2.0)
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.2.0")

    add("kapt", "androidx.room:room-compiler:2.6.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
}