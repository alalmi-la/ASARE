package com.example.applicationapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("AppDebug", "Application Started")

        try {
            // تهيئة Firebase
            FirebaseApp.initializeApp(this)
            Log.d("AppDebug", "Firebase Initialized")

            // تهيئة Cloudinary
            val cloudinaryConfig: HashMap<String, String> = HashMap()
            cloudinaryConfig["cloud_name"] = "dvnjmxoyx" // Key Name أو cloud_name
            cloudinaryConfig["api_key"] = "458556154423344"
            cloudinaryConfig["api_secret"] = "3kf2Mw-mkTl79D_KowMHB2Zubr4"
            MediaManager.init(this, cloudinaryConfig)
            Log.d("AppDebug", "Cloudinary Initialized")

            // إذا كنت تستخدم Firestore Emulator يمكنك تفعيلها
            // FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
            Log.d("AppDebug", "Firestore Ready")
        } catch (e: Exception) {
            Log.e("AppDebug", "Initialization Error", e)
        }
    }
}