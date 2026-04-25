plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sponsorflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sponsorflow"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        // VECTOR 20 DEFENSA: Reducción masiva de librerías C++ "Zombie" (-70% APK Size)
        // Llama y Whisper generan archivos .so pesados para 4 arquitecturas.
        // Forzamos solo ARM64 (el 99% de dispositivos móviles post-2018).
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    // Configuración robusta de empaquetado para NDK y ML/ONNX Libraries
    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
        jniLibs {
            // Evita colisiones de librerías nativas cuando varias dependencias traen el mismo .so (muy común en ONNX)
            pickFirsts += listOf("**/*.so")
        }
    }
    
    // Deshabilitar detector AutoboxingStateCreation que causa fallos en Lint con Compose
    lint {
        disable += "AutoboxingStateCreation"
        disable += "MutableCollectionMutableState"
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Jetpack Compose (Gestionado por BOM)
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Coroutines para nuestro "Load & Drop" asíncrono
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Room Database (La Memoria y Catálogo SQLite)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // ML Kit Text Recognition (El Ojo: Visión Offline para Capturas de Pantalla)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Inyección de Dependencias (Dagger Hilt + KSP)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // VECTOR 14 DEFENSA: WorkManager para el ResurrectorWorker
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    
    // Vector 22: ONNX Runtime para Semantic Match & Adaptive Thinking de Mythos
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.24.3")
    
    // DEPENDENCIAS DE PRUEBAS UNITARIAS Y RED TEAM TESTING
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
}
