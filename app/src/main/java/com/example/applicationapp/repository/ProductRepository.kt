package com.example.applicationapp.repository

import android.health.connect.datatypes.ExerciseRoute
import android.util.Log
import com.example.asare_montagrt.data.model.Product
import com.example.applicationapp.model.PriceHistory
import com.example.applicationapp.model.PriceRating
import com.example.applicationapp.model.Store
import com.example.applicationapp.screens.Menu.AppNotification
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference


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
    suspend fun addOrUpdateProduct(product: Product, userId: String): Boolean {
        val existing = getProductByBarcodeAndStore(
            product.barcode, product.storeName, product.storeLocation!!
        )

        return if (existing == null) {
            // 🟢 إضافة منتج جديد
            val docRef = productsCollection.add(product).await()
            val newId = docRef.id
            val updatedProduct = product.copy(id = newId)

            recordPriceHistory(updatedProduct, userId)

            sendNotificationToAll(
                title = "🆕 منتج جديد",
                message = "تمت إضافة المنتج \"${updatedProduct.name}\" في متجر ${updatedProduct.storeName}",
                productId = updatedProduct.id
            )

            true
        } else {
            // 🟡 تحديث منتج موجود
            productsCollection.document(existing.id)
                .update("price", product.price, "updatedAt", System.currentTimeMillis())
                .await()

            val updatedProduct = product.copy(id = existing.id)

            recordPriceHistory(updatedProduct, userId)

            sendNotificationToAll(
                title = "💲 تحديث سعر",
                message = "تم تحديث سعر المنتج \"${updatedProduct.name}\" في متجر ${updatedProduct.storeName}",
                productId = updatedProduct.id
            )

            false
        }
    }


    suspend fun getNotifications(userId: String): List<AppNotification> {
        val all = firestore.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        val dismissed = firestore.collection("users")
            .document(userId)
            .collection("dismissed_notifications")
            .get().await()
            .documents.map { it.id }

        return all.documents
            .filter { it.id !in dismissed }
            .map { doc ->
                AppNotification(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    productId = doc.getString("productId")
                )
            }
    }
    suspend fun markNotificationDismissed(userId: String, notificationId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("dismissed_notifications")
            .document(notificationId)
            .set(mapOf("dismissed" to true))
            .await()
    }










    // تخزين سجل السعر الجديد
    private suspend fun recordPriceHistory(product: Product, userId: String): DocumentReference {
        val geoPoint = com.google.firebase.firestore.GeoPoint(
            product.storeLocation!!.latitude,
            product.storeLocation.longitude
        )

        val priceEntry = mapOf(
            "productId"     to product.id,
            "storeName"     to product.storeName,
            "storeLocation" to geoPoint,
            "userId"        to userId,
            "price"         to product.price,
            "timestamp"     to System.currentTimeMillis(),
            "averageRating" to 0.0,
            "ratingsCount"  to 0
        )

        return firestore.collection("products")
            .document(product.id)
            .collection("price_history")
            .add(priceEntry)
            .await()
    }

    /** يضمن وجود سجل سعر ثم يضيف أو يحدث التقييم دفعة واحدة */
    suspend fun recordAndRatePrice(product: Product, rating: Float): Boolean {
        return try {
            val userId = FirebaseAuth.getInstance().uid ?: return false
            if (product.storeLocation == null) return false

            val historyRef: DocumentReference = getLastPriceHistoryDoc(product.id, product.storeName)
                ?: recordPriceHistory(product, userId)

            val historyId = historyRef.id

            submitPriceHistoryRating(product.id, historyId, userId, rating)
        } catch (e: Exception) {
            Log.e("FIRESTORE", "Error in recordAndRatePrice", e)
            false
        }
    }

    /** ترجع آخر وثيقة PriceHistory لكل متجر مرتبّ نزولياً */
    private suspend fun getLastPriceHistoryDoc(
        productId: String,
        storeName: String
    ): DocumentReference? {
        val snap = firestore.collection("products")
            .document(productId)
            .collection("price_history")
            .whereEqualTo("storeName", storeName)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        return snap.documents.firstOrNull()?.reference
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
        val data = snapshot.data ?: return null
        return parseProduct(snapshot.id, data)

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

    /** تسجيل تقييم على سجل سعر محدد */
    suspend fun submitPriceHistoryRating(
        productId: String,
        historyId: String,
        userId: String,
        rating: Float
    ): Boolean {
        val historyRef = firestore.collection("products")
            .document(productId)
            .collection("price_history")
            .document(historyId)

        try {
            // تحقق إذا كانت الوثيقة موجودة قبل التحديث
            val docSnapshot = historyRef.get().await()
            if (!docSnapshot.exists()) {
                Log.e("Firestore", "Document does not exist")
                return false
            }

            // حفظ التقييم
            historyRef.collection("ratings")
                .document(userId)
                .set(
                    mapOf(
                        "historyId" to historyId,
                        "userId"    to userId,
                        "rating"    to rating,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()

            // الحصول على التقييمات وحساب المتوسط
            val ratingsSnap = historyRef.collection("ratings").get().await()
            val ratings = ratingsSnap.documents.mapNotNull {
                it.getDouble("rating")?.toFloat()
            }
            val average = if (ratings.isNotEmpty()) ratings.average() else 0.0
            val count = ratings.size

            // تحديث بيانات التقييم
            historyRef.update(
                "averageRating", average,
                "ratingsCount", count
            ).await()

            return true
        } catch (e: Exception) {
            Log.e("Firestore", "Error in submitting price history rating", e)
            return false
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
    suspend fun sendNotificationToAll(title: String, message: String, productId: String? = null) {
        val notification = mapOf(
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "productId" to productId
        )
        firestore.collection("notifications").add(notification).await()
    }




    /** سجل تاريخ الأسعار لمنتج */
    suspend fun getPriceHistory(productId: String): List<PriceHistory> {
        val snap = firestore.collection("products")
            .document(productId)
            .collection("price_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        return snap.documents.map { doc ->
            PriceHistory(
                id            = doc.id,
                productId     = doc.getString("productId") ?: "",
                storeName     = doc.getString("storeName") ?: "",
                userId        = doc.getString("userId") ?: "",
                price         = doc.getDouble("price") ?: 0.0,
                timestamp     = doc.getLong("timestamp") ?: 0L,
                averageRating = doc.getDouble("averageRating") ?: 0.0,
                ratingsCount  = (doc.getLong("ratingsCount") ?: 0L).toInt()
            )
        }
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

            )
        } catch (_: Exception) {
            null
        }
    }
}
