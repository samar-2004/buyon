import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun readConfig(key: String): String {
    val localValue = localProperties.getProperty(key)
    if (!localValue.isNullOrBlank()) return localValue
    return System.getenv(key) ?: ""
}

android {
    namespace = "com.buyon.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.buyon.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val cloudName = readConfig("CLOUDINARY_CLOUD_NAME")
        val uploadPreset = readConfig("CLOUDINARY_UPLOAD_PRESET")

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$uploadPreset\"")
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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)
    implementation(libs.glide)
    implementation(libs.play.services.auth)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.4.0")
    // Shimmer for loading states
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
