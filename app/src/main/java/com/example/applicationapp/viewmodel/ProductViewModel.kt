package com.example.applicationapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applicationapp.repository.ProductRepository
import com.example.asare_montagrt.data.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList

    private val _productDetails = MutableStateFlow<Product?>(null)
    val productDetails: StateFlow<Product?> = _productDetails

    init {
        loadProducts()
    }

    // ✅ تحميل جميع المنتجات
    fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllProductsFlow().collectLatest { products ->
                _productList.value = products
            }
        }
    }

    // ✅ إضافة منتج جديد
    fun addProduct(product: Product) {
        viewModelScope.launch {
            productRepository.addProduct(product)
            loadProducts() // تحديث القائمة بعد الإضافة
        }
    }

    // ✅ تحديث منتج
    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
            loadProducts() // تحديث القائمة بعد التحديث
        }
    }

    // ✅ جلب منتج حسب ID (مع تجنب NullPointerException)
    fun getProductById(productId: String) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            _productDetails.value = product ?: Product() // ✅ إذا كان المنتج غير موجود، يتم إرجاع كائن فارغ لتجنب الأخطاء
        }
    }

    // ✅ حذف منتج
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            loadProducts() // تحديث القائمة بعد الحذف
        }
    }
}
