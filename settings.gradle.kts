// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // MapTiler SDK repository
        maven { url = uri("https://api.maptiler.com/maven/") }

        // MapLibre (إذا لا زلت تحتاجه)
        maven { url = uri("https://maplibre.org/maven") }

        // JitPack
        maven { url = uri("https://jitpack.io") }

        // لقراءة ملفات .aar من libs/
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "ApplicationAPP"
include(":app")
