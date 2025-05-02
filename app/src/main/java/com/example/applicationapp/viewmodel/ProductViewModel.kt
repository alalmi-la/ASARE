package com.example.applicationapp.viewmodel

import User
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applicationapp.model.Store
import com.example.applicationapp.repository.ProductRepository
import com.example.asare_montagrt.data.model.Product
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class SortType(val displayName: String) {
    ALL("الكل"),
    TOP_RATED("الأكثر تقييماً"),
    FEATURED("منتجات بارزة"),
    NEARBY("الأقرب إليك")
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList

    private val _productDetails = MutableStateFlow<Product?>(null)
    val productDetails: StateFlow<Product?> = _productDetails

    private val _selectedProducts = MutableStateFlow<List<Product>>(emptyList())
    val selectedProducts: StateFlow<List<Product>> = _selectedProducts

    private val _pickedLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val pickedLocation: StateFlow<Pair<Double, Double>?> = _pickedLocation

    private val _storeList = MutableStateFlow<List<Store>>(emptyList())
    val storeList: StateFlow<List<Store>> = _storeList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _currentSortType = MutableStateFlow(SortType.ALL)
    val currentSortType: StateFlow<SortType> = _currentSortType

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions

    private val recentSearches = mutableListOf<Product>()
    private var currentProductLimit = 50L
    private var userLocation: Location? = null

    init {
        loadProducts()
        loadStores()
    }

    fun setUser(user: User) {
        _currentUser.value = user
    }

    fun logout(onLoggedOut: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        _currentUser.value = null
        onLoggedOut()
    }

    fun getProductsByName(name: String): List<Product> =
        _productList.value.filter { it.name.contains(name, ignoreCase = true) }

    fun getProductsByBarcodeNow(barcode: String): List<Product> =
        _productList.value.filter { it.barcode == barcode }

    fun fetchAutocompleteSuggestions(query: String) {
        viewModelScope.launch {
            val suggestions = productRepository.getProductNames(query)
            _searchSuggestions.value = suggestions
        }
    }

    fun loadProducts(limit: Long = currentProductLimit) {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getProductsFlow(limit).collectLatest { products ->
                _productList.value = products
                _isLoading.value = false
            }
        }
    }

    fun loadMoreProducts() {
        currentProductLimit += 50
        loadProducts(currentProductLimit)
    }

    fun loadStores() {
        viewModelScope.launch {
            productRepository.getAllStoresFlow().collectLatest { stores ->
                _storeList.value = stores
            }
        }
    }

    fun updateSortType(sortType: SortType) {
        _currentSortType.value = sortType
    }

    fun getSortedProducts(): List<Product> {
        val products = _productList.value
        return when (_currentSortType.value) {
            SortType.ALL -> products
            SortType.TOP_RATED -> products.sortedByDescending { it.rating }
            SortType.FEATURED -> products.sortedByDescending { it.updatedAt }
            SortType.NEARBY -> {
                userLocation?.let { loc ->
                    products.sortedBy { product ->
                        product.storeLocation?.let { geo ->
                            val storeLoc = Location("store").apply {
                                latitude = geo.latitude
                                longitude = geo.longitude
                            }
                            loc.distanceTo(storeLoc).toDouble()
                        } ?: Double.MAX_VALUE
                    }
                } ?: products
            }
        }
    }

    fun getProductsSorted(
        products: List<Product>,
        sortOption: String,
        userLocation: Location?
    ): List<Product> {
        return when (sortOption) {
            "الأرخص" -> products.sortedBy { it.price }
            "الأحدث" -> products.sortedByDescending { it.updatedAt }
            "الأقرب" -> {
                userLocation?.let { loc ->
                    products.sortedBy {
                        it.storeLocation?.let { geo ->
                            val storeLoc = Location("store").apply {
                                latitude = geo.latitude
                                longitude = geo.longitude
                            }
                            loc.distanceTo(storeLoc).toDouble()
                        } ?: Double.MAX_VALUE
                    }
                } ?: products
            }
            else -> products
        }
    }

    fun getStoreDistanceInKm(userLocation: Location?, storeLocation: GeoPoint?): Double? {
        if (userLocation == null || storeLocation == null) return null
        val storeLoc = Location("store").apply {
            latitude = storeLocation.latitude
            longitude = storeLocation.longitude
        }
        return userLocation.distanceTo(storeLoc) / 1000.0
    }

    fun getTopRatedProducts(): List<Product> = getSortedProducts()
    fun getFeaturedProducts(): List<Product> = _productList.value.sortedByDescending { it.updatedAt }
    fun getRecentSearches(): List<Product> = recentSearches.reversed()
    fun recordProductSearch(product: Product) {
        if (!recentSearches.contains(product)) {
            recentSearches.add(product)
            if (recentSearches.size > 10) recentSearches.removeFirst()
        }
    }

    fun getCurrentUserLocation(context: Context) {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val client = LocationServices
                        .getFusedLocationProviderClient(context)
                    val location = client.lastLocation.await()
                    userLocation = location
                } catch (_: Exception) { }
            }
        }
    }

    fun getUserLocation(): Location? = userLocation

    fun setPickedLocation(latitude: Double, longitude: Double) {
        _pickedLocation.value = Pair(latitude, longitude)
    }

    fun addProductFromUI(
        name: String,
        price: Double,
        storeName: String,
        barcode: String,
        imageUrl: String
    ) {
        val newProduct = Product(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            price = price,
            storeName = storeName,
            barcode = barcode,
            imageUrl = imageUrl
        )
        viewModelScope.launch {
            productRepository.addProduct(newProduct)
            loadProducts()
        }
    }

    fun addSelectedProduct(product: Product) {
        val currentList = _selectedProducts.value.toMutableList()
        currentList.add(product)
        _selectedProducts.value = currentList
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
            loadProducts()
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            loadProducts()
        }
    }

    fun getProductByIdNow(productId: String): Product? =
        _productList.value.find { it.id == productId }

    fun checkAndAddStore(
        name: String,
        location: GeoPoint,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val exists = productRepository.doesStoreExist(name, location)
            if (!exists) productRepository.addStore(name, location)
            onResult(exists)
        }
    }

    fun submitPriceRating(
        barcode: String,
        storeName: String,
        rating: Float,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: return@launch
            val success = productRepository
                .submitPriceRating(barcode, storeName, userId, rating.toInt())
            onResult(success)
        }
    }

    suspend fun getAverageRating(barcode: String, storeName: String): Float =
        productRepository.getAveragePriceRating(barcode, storeName).toFloat()

    suspend fun getUserRating(
        barcode: String,
        storeName: String,
        userId: String
    ): Float? = productRepository
        .getUserPriceRating(barcode, storeName, userId)
        ?.toFloat()

    suspend fun getPriceHistory(
        barcode: String,
        storeName: String
    ): List<Pair<Long, Double>> =
        productRepository.getPriceHistory(barcode, storeName)
}
