# -------------------------------------------------------------------------
# SPONSORFLOW V2.0 - SRE & KERNEL LEVEL OBFUSCATION / PROGUARD SHIELD
# -------------------------------------------------------------------------

# VECTOR 10 DEFENSA: Anti-Reverse Engineering para el Security Vault
# Protegemos el Vault para evitar manipulaciones con Frida o Xposed Framework.
-keep class com.sponsorflow.security.SecurityVault { *; }

# VECTOR 6 DEFENSA JNI: Evitamos que el compilador desate los puentes C++ a Llama / Whisper.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.sponsorflow.core.LlamaEngine { *; }
-keep class com.sponsorflow.core.VoiceEngine { *; }

# VECTOR 8 DEFENSA SQL: Proteger Room WAL y sus DAOs (Reflection usage by Google)
-keep class com.sponsorflow.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# Prevenir recortes de la inyección nativa de Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# VECTOR 22 DEFENSA: ONNX Runtime
# Obligatorio para evitar que R8 "Full Mode" mutile las interfaces JNI de la Inteligencia Artificial Local (ONNX)
-keep class ai.onnxruntime.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }

# VECTOR 12 DEFENSA: Silenciar Warnings de librerías comunes
-dontwarn androidx.work.**
-dontwarn androidx.room.**
-dontwarn androidx.lifecycle.**
-dontwarn com.google.android.gms.**
-dontwarn android.support.**
-dontwarn com.google.android.vending.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.ui.**
-dontwarn androidx.versionedparcelable.**
-dontwarn com.google.firebase.**
-dontwarn androidx.startup.**
