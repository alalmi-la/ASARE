package com.example.applicationapp.model

data class PriceHistory(
    val productId: String = "",
    val storeName: String = "",
    val price: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

