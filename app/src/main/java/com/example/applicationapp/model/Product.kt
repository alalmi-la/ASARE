package com.example.asare_montagrt.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val store: String = "",
    val barcode: String = "",
    val imageUrl: String = "",  // ✅ تم إضافة `imageUrl`
    val priceHistory: MutableList<Double> = mutableListOf()
)

