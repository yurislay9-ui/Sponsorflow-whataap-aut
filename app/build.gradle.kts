plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Procesador de anotaciones nativo para Room DB
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sponsorflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sponsorflow"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        // VECTOR 20 DEFENSA: Reducción masiva de librerías C++ "Zombie" (-70% APK Size)
        // Llama y Whisper generan archivos .so pesados para 4 arquitecturas.
        // Forzamos solo ARM64 (el 99% de dispositivos móviles post-2018).
        ndk {
            abiFilters.add("arm64-v8a")
        }
        
        // CONFIGURACIÓN CRÍTICA PARA GITHUB ACTIONS Y NDK C++
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O3"
                // Optimizamos NDK para compilar más ràpido en los servidores de GitHub
                arguments += "-DANDROID_ARM_MODE=arm"
            }
        }
    }

    // Le decimos a Gradle dónde está nuestro script de compilación C++
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        kotlinCompilerExtensionVersion = "1.5.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Dependencias de Jetpack Compose (UI Nativa)
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    
    // Coroutines para nuestro "Load & Drop" asíncrono
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room Database (La Memoria y Catálogo SQLite)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:\$room_version")
    implementation("androidx.room:room-ktx:\$room_version")
    kapt("androidx.room:room-compiler:\$room_version")

    // ML Kit Text Recognition (El Ojo: Visión Offline para Capturas de Pantalla)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // Para simplificar promesas a Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ViewModel para Jetpack Compose (Gestión de Estado)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Jetpack Security: Cifrado AES-256 anclado al Hardware Keystore para Anti-Piratería
    implementation("androidx.security:security-crypto:1.0.0")
    
    // VECTOR 14 DEFENSA: WorkManager para el ResurrectorWorker
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
