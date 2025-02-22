package com.example.applicationapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("AppDebug", "Application Started")

        try {
            FirebaseApp.initializeApp(this)
            Log.d("AppDebug", "Firebase Initialized")

            // ✅ Uncomment if using Firestore Emulator
            // FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)

            Log.d("AppDebug", "Firestore Ready")
        } catch (e: Exception) {
            Log.e("AppDebug", "Initialization Error", e)
        }
    }
}
