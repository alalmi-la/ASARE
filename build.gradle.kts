// build.gradle.kts (Project)

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }  // MapLibre
        maven { url = uri("https://api.maptiler.com/maven/") } // MapTiler
        maven { url = uri("https://jitpack.io") }
        flatDir {
            dirs("libs")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
    }
}

plugins {
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library")     version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}
