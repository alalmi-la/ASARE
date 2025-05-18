package com.example.applicationapp.model

data class PriceRating(
    val historyId: String = "",    // ربط التقييم بسجل السعر المحدد
    val userId: String = "",       // من قام بالتقييم
    val rating: Float = 0f,        // قيمة التقييم
    val timestamp: Long = System.currentTimeMillis(),
    val storeName: String = "",
    val productId: String = ""
)


