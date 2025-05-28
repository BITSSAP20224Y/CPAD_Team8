// app/build.gradle.kts (Module: app)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.currencyidentifier"
    compileSdk = 34 // Or latest stable API level

    defaultConfig {
        applicationId = "com.example.currencyidentifier"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        // Ensure you're using Java 17 for AGP 8.x and latest Kotlin versions
        sourceCompatibility = JavaVersion.VERSION_17 // Updated
        targetCompatibility = JavaVersion.VERSION_17 // Updated
    }
    kotlinOptions {
        jvmTarget = "17" // Updated
    }
    buildFeatures {
        compose = true
        // CRITICAL: Enable ML Model Binding here
        mlModelBinding = true // <--- ADD THIS LINE
    }
    composeOptions {
        // This version should match your Compose BOM version and your Kotlin version.
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    aaptOptions {
        noCompress("tflite") // This is good, but `mlModelBinding = true` is essential
    }

    configurations.all { // <--- ADD THIS BLOCK
        resolutionStrategy {
            force("org.tensorflow:tensorflow-lite-support:0.4.3")
        }
    }
}

dependencies {

    // Core Android KTX and Compose
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom)) // BOM for Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.material:material-icons-extended:1.6.7")// ViewModel for Compose
    // For observeAsState and LiveData integration with Compose
    implementation("androidx.compose.runtime:runtime-livedata") // <--- ADD THIS

    // NEW: Compose Material Icons Extended for CameraAlt, FileUpload icons
    implementation("androidx.compose.material:material-icons-extended") // <--- ADD THIS

    // CameraX
    val camerax_version = "1.3.3" // Check for latest stable version
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("androidx.camera:camera-extensions:$camerax_version")

    // TensorFlow Lite Core and GPU
    val tflite_core_version = "2.10.0"
    implementation("org.tensorflow:tensorflow-lite:$tflite_core_version")
    implementation("org.tensorflow:tensorflow-lite-gpu:$tflite_core_version")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")

    // CRITICAL: TensorFlow Lite Support Library (for ImageProcessor, TensorImage, NormalizeOp etc.)
    // Ensure this version is compatible. 0.4.3 is generally compatible with tflite 2.10.0
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3") // <--- ADD/UPDATE THIS

    // TensorFlow Lite Task Library (this version is fine)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}