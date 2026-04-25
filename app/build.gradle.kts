import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

val localProps: Properties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

fun localPropOrDefault(key: String, default: String): String =
    localProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: default

val backendBaseUrl: String = localPropOrDefault(
    "BACKEND_BASE_URL",
    "https://interactivemap-backend.vercel.app/",
)
val translateSourceLang: String = localPropOrDefault("TRANSLATE_SOURCE_LANG", "es-ES")
val translateTargetLang: String = localPropOrDefault("TRANSLATE_TARGET_LANG", "en-US")

android {
    namespace = "com.uniandes.interactivemapuniandes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uniandes.interactivemapuniandes"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
        buildConfigField("String", "TRANSLATE_SOURCE_LANG", "\"$translateSourceLang\"")
        buildConfigField("String", "TRANSLATE_TARGET_LANG", "\"$translateTargetLang\"")
    }

    buildFeatures {
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

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // Task<T>.await()
    implementation("androidx.datastore:datastore-preferences:1.1.3") // Local prefs
    implementation("androidx.core:core-splashscreen:1.0.1") // Splash screen API 31 compat
    implementation("androidx.recyclerview:recyclerview:1.3.2") // Used by Search + Schedules
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil:3.4.0") // imageView.load() extension for views-based layouts
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")
    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.13.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
