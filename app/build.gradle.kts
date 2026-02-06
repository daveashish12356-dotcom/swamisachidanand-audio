plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.swamisachidanand"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.swamisachidanand"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Support for different screen sizes
        vectorDrawables.useSupportLibrary = true

        // Build only for arm64-v8a (your device) to speed up builds
        ndk {
            abiFilters.add("arm64-v8a")
        }

        // 16 KB native library alignment for Android 15+ page size
        externalNativeBuild {
            cmake {
                arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "swami123"
            keyAlias = "release"
            keyPassword = "swami123"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false  // Temporarily disabled for faster builds
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        // Separate buildType so we can build a fresh APK even if app-release.apk is locked by Windows
        create("internal") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs {
            // false = uncompressed .so with 16 KB zip alignment (required for Play 16 KB page size)
            useLegacyPackaging = false
        }
    }

    androidResources {
        noCompress += "mp4"
    }
    
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.core)
    
    // ViewPager2 for smooth scrolling with 3D effects
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Google ML Kit Text Recognition (for general text)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    
    // Tesseract OCR for Gujarati (better support for scanned PDFs with Indic scripts)
    implementation("com.rmtheis:tess-two:9.1.0")
    
    // PDFBox Android - read PDF outline (chapters) for all books
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // OkHttp for Google Cloud Text-to-Speech API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Firebase (BOM manages versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.common)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
