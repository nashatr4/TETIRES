plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)  // ← GANTI dari kapt ke ksp
    id("com.chaquo.python")
}

android {
    namespace = "com.example.tetires"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tetires"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    flavorDimensions += "pyVersion"
    productFlavors {
        create("py311") {
            dimension = "pyVersion"
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            install("numpy")
            install("pandas")
        }
    }
    productFlavors {
        getByName("py311") {
            version = "3.11"
        }
    }
}

dependencies {
    implementation("com.github.mik3y:usb-serial-for-android:3.7.3")

    val navVersion = "2.9.4"
    val roomVersion = "2.6.1"

    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Room ORM - GANTI kapt dengan ksp
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")  // ← GANTI dari kapt ke ksp

    // Material icons
    implementation("androidx.compose.material:material-icons-core:1.6.8")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ConstraintLayout Compose
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // Compose Runtime LiveData
    implementation("androidx.compose.runtime:runtime-livedata:1.9.2")


    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}