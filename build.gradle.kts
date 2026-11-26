// Top-level build file — CLOUD variant (Firebase)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // NOTE: no org.jetbrains.kotlin.plugin.compose plugin for Kotlin 1.9.x —
    // Compose uses composeOptions.kotlinCompilerExtensionVersion below (1.5.14).
    // No KSP plugin: the cloud variant has no Room, so no annotation processing.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
