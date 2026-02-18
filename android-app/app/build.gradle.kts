import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

// Carrega local.properties com segurança — nunca commite esse arquivo
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.rcdownload"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rcdownload"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Injeta a API Key e URL do serviço local no BuildConfig
        // Nunca hardcode esses valores no código-fonte
        buildConfigField(
            "String", "YOUTUBE_API_KEY",
            "\"${localProperties.getProperty("YOUTUBE_API_KEY", "")}\""
        )
        buildConfigField(
            "String", "LOCAL_SERVICE_URL",
            // 10.0.2.2 aponta para localhost do host quando rodando no emulador Android
            "\"${localProperties.getProperty("LOCAL_SERVICE_URL", "http://10.0.2.2:8080/")}\""
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ViewModel + coroutines
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit + OkHttp (chamadas HTTP à YouTube API e ao serviço local)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Room (histórico de downloads)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Coil (carregamento de thumbnails)
    implementation(libs.coil)

    // --- Testes unitários ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // --- Testes instrumentados ---
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
