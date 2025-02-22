buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25") // ✅ متوافق مع Compose 1.5.15
        classpath("com.google.gms:google-services:4.3.15")           // ✅ Firebase
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1") // ✅ Hilt
    }
}

plugins {
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false // ✅ متطابق مع أعلاه
}
