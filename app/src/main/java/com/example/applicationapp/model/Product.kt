package com.example.asare_montagrt.data.model

import com.google.firebase.firestore.GeoPoint

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
    val priceHistory: List<Double> = emptyList(),
    val storeName: String = "",
    val storeLocation: GeoPoint? = null,
    val barcode: String = "",
    val brand: String = "" ,// ✅ حقل جديد مضاف بأمان
    val rating: Double = 0.0 // ⭐️ التقييم من 0 إلى 5

)


