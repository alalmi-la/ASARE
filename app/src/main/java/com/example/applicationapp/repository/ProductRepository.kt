package com.example.applicationapp.repository

import com.example.asare_montagrt.data.model.Product
import com.example.applicationapp.model.PriceHistory
import com.example.applicationapp.model.PriceRating
import com.example.applicationapp.model.Store
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val productsCollection = firestore.collection("products")
    private val storesCollection = firestore.collection("stores")
    private val priceRatingsCollection = firestore.collection("price_ratings")
    private val priceHistoryCollection = firestore.collection("price_history")

    suspend fun addProduct(product: Product) {
        productsCollection.add(product).await()
    }

    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id).set(product).await()
    }

    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }

    suspend fun getProductById(productId: String): Product? {
        val snapshot = productsCollection.document(productId).get().await()
        return snapshot.toObject(Product::class.java)?.copy(id = snapshot.id)
    }

    suspend fun getProducts(limit: Long = 50): List<Product> {
        val snapshot = productsCollection.limit(limit).get().await()
        return snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
    }

    fun getProductsFlow(limit: Long = 50): Flow<List<Product>> = flow {
        emit(getProducts(limit))
    }

    suspend fun getProductsByBarcode(barcode: String): List<Product> {
        val snapshot = productsCollection.whereEqualTo("barcode", barcode).get().await()
        val matched = snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
        return if (matched.isEmpty() && barcode.isNotBlank()) {
            getProductsByName(barcode)
        } else matched
    }

    suspend fun getProductsByName(name: String): List<Product> {
        val snapshot = productsCollection.whereEqualTo("name", name).get().await()
        return snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
    }

    suspend fun getProductByBarcodeAndStore(
        barcode: String,
        storeName: String,
        storeLocation: GeoPoint
    ): Product? {
        val snapshot = productsCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", storeName)
            .whereEqualTo("storeLocation", storeLocation)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { parseProduct(it.id, it.data) }
    }

    suspend fun doesStoreExist(name: String, location: GeoPoint): Boolean {
        val snapshot = storesCollection
            .whereEqualTo("name", name)
            .whereEqualTo("location", location)
            .get()
            .await()

        return !snapshot.isEmpty
    }

    suspend fun addStore(name: String, location: GeoPoint) {
        storesCollection
            .add(mapOf("name" to name, "location" to location))
            .await()
    }

    fun getAllStoresFlow(): Flow<List<Store>> = flow {
        val snapshot = storesCollection.get().await()
        emit(snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name")
            val location = doc.getGeoPoint("location")
            if (name != null && location != null) {
                Store(name, location.latitude, location.longitude)
            } else null
        })
    }

    suspend fun submitPriceRating(barcode: String, storeName: String, userId: String, rating: Int): Boolean {
        val doc = priceRatingsCollection.document("$barcode-$storeName-$userId")
        return try {
            doc.set(
                PriceRating(
                    productId = barcode,
                    storeName = storeName,
                    userId = userId,
                    rating = rating.toFloat(),
                    timestamp = System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun getProductNames(query: String): List<String> {
        val snapshot = productsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.getString("name") }
    }



    suspend fun getAveragePriceRating(barcode: String, storeName: String): Double {
        val snapshot = priceRatingsCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", storeName)
            .get().await()

        val ratings = snapshot.documents.mapNotNull {
            it.getLong("rating")?.toInt()
        }

        return if (ratings.isNotEmpty()) {
            ratings.average()
        } else 0.0
    }

    // ✅ الدالة الجديدة: تقييم المستخدم للسعر
    suspend fun getUserPriceRating(barcode: String, storeName: String, userId: String): Int? {
        val docId = "$barcode-$storeName-$userId"
        val snapshot = priceRatingsCollection.document(docId).get().await()
        return snapshot.getLong("rating")?.toInt()
    }

    // ✅ جلب سجل السعر بصيغة (timestamp to price)
    suspend fun getPriceHistory(barcode: String, storeName: String): List<Pair<Long, Double>> {
        val snapshot = priceHistoryCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", storeName)
            .get().await()

        return snapshot.documents.mapNotNull {
            try {
                val timestamp = it.getLong("timestamp") ?: 0L
                val price = it.getDouble("price") ?: 0.0
                timestamp to price
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.first }
    }

    private fun parseProduct(id: String, data: Map<String, Any>?): Product? {
        return try {
            if (data == null) return null
            Product(
                id = id,
                name = data["name"] as? String ?: "",
                price = data["price"] as? Double ?: 0.0,
                imageUrl = data["imageUrl"] as? String ?: "",
                priceHistory = (data["priceHistory"] as? List<*>)?.mapNotNull { it as? Double } ?: emptyList(),
                storeName = data["storeName"] as? String ?: "",
                storeLocation = data["storeLocation"] as? GeoPoint,
                barcode = data["barcode"] as? String ?: "",
                brand = data["brand"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
