package com.example.applicationapp.model

import com.google.firebase.firestore.GeoPoint

data class PriceHistory(
    val id: String = "",             // معرف الوثيقة في Firestore
    val productId: String = "",      // ربط بالسمة id للمنتج
    val storeId: String = "",        // جديد: ربط بمعرّف المتجر
    val storeName: String = "",      // اسم المتجر (للعرض السريع)
    val storeLocation: GeoPoint? = null,  // جديد: الموقع الجغرافي
    val userId: String = "",         // من أضاف هذا السعر
    val price: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val averageRating: Double = 0.0, // متوسط تقييم هذا السعر
    val ratingsCount: Int = 0        // عدد التقييمات
)


