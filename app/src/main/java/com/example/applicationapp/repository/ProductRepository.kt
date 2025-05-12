package com.example.applicationapp.repository

import android.health.connect.datatypes.ExerciseRoute
import com.example.asare_montagrt.data.model.Product
import com.example.applicationapp.model.PriceHistory
import com.example.applicationapp.model.PriceRating
import com.example.applicationapp.model.Store
import com.example.applicationapp.viewmodel.SortType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // المجموعات الأصلية
    private val productsCollection     = firestore.collection("products")
    private val storesCollection       = firestore.collection("stores")
    private val priceRatingsCollection = firestore.collection("price_ratings")
    private val priceHistoryCollection = firestore.collection("price_history")
    private var lastDocumentSnapshot: DocumentSnapshot? = null

    // ────────────────────────────────────
    // دوال CRUD الأصلية
    // ────────────────────────────────────

    /** إضافة منتج جديد وتسجيل السعر في سجل الأسعار */
    /**
     * يضيف أو يحدث المنتج نفسه (barcode + store) ثم يسجل التاريخ.
     * @return true إذا أُضيف جديد، false إذا حدّثنا موجود.
     */
    suspend fun addOrUpdateProduct(product: Product): Boolean {
        val existing = getProductByBarcodeAndStore(
            product.barcode, product.storeName, product.storeLocation!!
        )
        return if (existing == null) {
            productsCollection.add(product).await()
            recordPriceHistory(product)
            true
        } else {
            productsCollection.document(existing.id)
                .update("price", product.price, "updatedAt", System.currentTimeMillis())
                .await()
            recordPriceHistory(product)
            false
        }
    }

    private suspend fun recordPriceHistory(product: Product) {
        priceHistoryCollection.add(
            mapOf(
                "barcode"   to product.barcode,
                "storeName" to product.storeName,
                "price"     to product.price,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
    }


    /** تحديث بيانات منتج موجود */
    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id).set(product).await()
    }

    /** حذف منتج */
    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }

    /** الحصول على منتج بالمعرف */
    suspend fun getProductById(productId: String): Product? {
        val snapshot = productsCollection.document(productId).get().await()
        return snapshot.toObject(Product::class.java)?.copy(id = snapshot.id)
    }

    /** استعلام بسيط مع حد */
    suspend fun getProducts(limit: Long = 50): List<Product> {
        val snapshot = productsCollection.limit(limit).get().await()
        return snapshot.documents.mapNotNull { doc ->
            parseProduct(doc.id, doc.data)
        }
    }

    /** دفق (Flow) للمنتجات مع حد */
    fun getProductsFlow(limit: Long = 50): Flow<List<Product>> = flow {
        emit(getProducts(limit))
    }

    /** البحث عن منتجات بواسطة باركود */
    suspend fun getProductsByBarcode(barcode: String): List<Product> {
        val snapshot = productsCollection
            .whereEqualTo("barcode", barcode)
            .get()
            .await()
        val matched = snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
        return if (matched.isEmpty() && barcode.isNotBlank()) {
            getProductsByName(barcode)
        } else matched
    }

    /** البحث عن منتجات بواسطة الاسم */
    suspend fun getProductsByName(name: String): List<Product> {
        val snapshot = productsCollection
            .whereEqualTo("name", name)
            .get()
            .await()
        return snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
    }

    /** الحصول على منتج بواسطة باركود ومتجر */
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

    /** التحقق من وجود متجر */
    suspend fun doesStoreExist(name: String, location: GeoPoint): Boolean {
        val snapshot = storesCollection
            .whereEqualTo("name", name)
            .whereEqualTo("location", location)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    /** إضافة متجر جديد */
    suspend fun addStore(name: String, location: GeoPoint) {
        storesCollection.add(mapOf("name" to name, "location" to location)).await()
    }

    /** تدفق (Flow) لجميع المتاجر */
    fun getAllStoresFlow(): Flow<List<Store>> = flow {
        val snapshot = storesCollection.get().await()
        emit(snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name")
            val geo  = doc.getGeoPoint("location")
            if (name != null && geo != null) Store(name, geo.latitude, geo.longitude)
            else null
        })
    }

    /** تسجيل تقييم سعر */
    suspend fun submitPriceRating(
        barcode: String,
        storeName: String,
        userId: String,
        rating: Int
    ): Boolean {
        return try {
            priceRatingsCollection
                .document("$barcode-$storeName-$userId")
                .set(
                    PriceRating(
                        productId = barcode,
                        storeName = storeName,
                        userId = userId,
                        rating = rating.toFloat(),
                        timestamp = System.currentTimeMillis()
                    )
                )
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** اقتراح أسماء المنتجات (Autocomplete) */
    suspend fun getProductNames(query: String): List<String> {
        val snapshot = productsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.getString("name") }
    }

    /** متوسط التقييمات لمنتج */
    suspend fun getAveragePriceRating(barcode: String, storeName: String): Double {
        val snapshot = priceRatingsCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", storeName)
            .get()
            .await()

        val ratings = snapshot.documents.mapNotNull { it.getDouble("rating") }
        return if (ratings.isNotEmpty()) ratings.average() else 0.0
    }


    /** تقييم المستخدم الخاص */
    suspend fun getUserPriceRating(barcode: String, storeName: String, userId: String): Int? {
        val docId   = "$barcode-$storeName-$userId"
        val snapshot = priceRatingsCollection.document(docId).get().await()
        return snapshot.getLong("rating")?.toInt()
    }

    /** سجل تاريخ الأسعار لمنتج */
    suspend fun getPriceHistory(barcode: String, storeName: String): List<Pair<Long, Double>> {
        val snapshot = priceHistoryCollection
            .whereEqualTo("barcode", barcode)
            .whereEqualTo("storeName", storeName)
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            val ts    = it.getLong("timestamp") ?: return@mapNotNull null
            val price = it.getDouble("price")    ?: return@mapNotNull null
            ts to price
        }.sortedBy { it.first }
    }

    // ────────────────────────────────────
    // دوال Pagination الجديدة فقط
    // ────────────────────────────────────

    /** تحميل أول صفحة من المنتجات مرتبة تنازليًا حسب updatedAt */
    suspend fun getFirstPage(pageSize: Int): List<Product> {
        val query = productsCollection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())
        val snapshot = query.get().await()
        lastDocumentSnapshot = snapshot.documents.lastOrNull()
        return snapshot.toObjects(Product::class.java).map { it.copy(id = it.id) }
    }

    /** تحميل الصفحة التالية بناءً على آخر مستند */
    suspend fun getNextPage(pageSize: Int): List<Product> {
        val startAfter = lastDocumentSnapshot ?: return emptyList()
        val query = productsCollection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .startAfter(startAfter)
            .limit(pageSize.toLong())
        val snapshot = query.get().await()
        lastDocumentSnapshot = snapshot.documents.lastOrNull()
        return snapshot.toObjects(Product::class.java).map { it.copy(id = it.id) }
    }
    suspend fun getFilteredProducts(
        name: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        storeName: String? = null
    ): List<Product> {
        var query: Query = productsCollection

        // فلترة حسب الاسم (يُعامل كـ prefix)
        if (!name.isNullOrBlank()) {
            query = query
                .whereGreaterThanOrEqualTo("name", name)
                .whereLessThanOrEqualTo("name", name + '\uf8ff')
        }

        // فلترة حسب السعر
        if (minPrice != null) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice)
        }
        if (maxPrice != null) {
            query = query.whereLessThanOrEqualTo("price", maxPrice)
        }

        // فلترة حسب اسم المتجر
        if (!storeName.isNullOrBlank()) {
            query = query.whereEqualTo("storeName", storeName)
        }

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
    }
    suspend fun getProductsSortedByPrice(descending: Boolean = false): List<Product> {
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val snapshot = productsCollection.orderBy("price", direction).get().await()
        return snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }
    }

    suspend fun getProductsSortedByRating(): List<Product> {
        val snapshot = productsCollection.get().await()
        val products = snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }

        return products.map { product ->
            val rating = getAveragePriceRating(product.barcode, product.storeName)
            product to rating
        }.sortedByDescending { it.second }
            .map { it.first }
    }

    suspend fun getProductsSortedByNearest(userLocation: GeoPoint): List<Product> {
        val snapshot = productsCollection.get().await()
        val products = snapshot.documents.mapNotNull { parseProduct(it.id, it.data) }

        return products.sortedBy { product ->
            product.storeLocation?.let { location ->
                distanceBetween(userLocation, location)
            } ?: Double.MAX_VALUE
        }
    }

    // 🔹 دالة حساب المسافة بين نقطتين
    private fun distanceBetween(p1: GeoPoint, p2: GeoPoint): Double {
        val dx = p1.latitude - p2.latitude
        val dy = p1.longitude - p2.longitude
        return sqrt(dx.pow(2) + dy.pow(2))
    }




    // ────────────────────────────────────
    // محول بيانات Firestore خام إلى Product
    // ────────────────────────────────────
    private fun parseProduct(id: String, data: Map<String, Any>?): Product? {
        return try {
            if (data == null) return null
            Product(
                id            = id,
                name          = data["name"] as? String ?: "",
                price         = data["price"] as? Double ?: 0.0,
                imageUrl      = data["imageUrl"] as? String ?: "",
                priceHistory  = (data["priceHistory"] as? List<*>)?.mapNotNull { it as? Double }
                    ?: emptyList(),
                storeName     = data["storeName"] as? String ?: "",
                storeLocation = data["storeLocation"] as? GeoPoint,
                barcode       = data["barcode"] as? String ?: "",
                brand         = data["brand"] as? String ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }
}
