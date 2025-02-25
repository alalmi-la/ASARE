package com.example.applicationapp.repository

import kotlinx.coroutines.channels.awaitClose
import com.example.asare_montagrt.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val productsCollection = firestore.collection("products")

    fun getAllProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val products = snapshot?.toObjects(Product::class.java) ?: emptyList()
            trySend(products).isSuccess
        }
        awaitClose { listener.remove() }
    }

    suspend fun addProduct(product: Product) {
        val newProduct = product.copy(id = firestore.collection("products").document().id)
        productsCollection.document(newProduct.id).set(newProduct).await()
    }

    suspend fun updateProduct(product: Product) {
        product.id.takeIf { it.isNotEmpty() }?.let {
            productsCollection.document(it).set(product).await()
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return productsCollection.document(productId).get().await().toObject(Product::class.java)
    }

    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }

    // دالة البحث باستخدام الباركود وبيانات المتجر
    suspend fun getProductByBarcodeAndStore(barcode: String, store: String, storeLocation: String): Product? {
        val querySnapshot = productsCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", store)
            .whereEqualTo("storeLocation", storeLocation)
            .get()
            .await()
        return if (querySnapshot.documents.isNotEmpty()) {
            querySnapshot.documents.first().toObject(Product::class.java)
        } else null
    }
}
