package com.example.applicationapp.model

data class PriceRating(
    val productId: String = "",
    val storeName: String = "",
    val userId: String = "",
    val rating: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

