package com.example.asare_montagrt.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val storeName: String = "",       // تمت إعادة التسمية من "store" إلى "storeName"
    val storeLocation: String = "",   // خاصية جديدة لموقع المتجر
    val barcode: String = "",
    val imageUrl: String = "",
    val priceHistory: MutableList<Double> = mutableListOf()
)
