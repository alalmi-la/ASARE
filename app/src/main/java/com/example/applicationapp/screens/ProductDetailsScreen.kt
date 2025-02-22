package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment // ✅ إضافة الاستيراد الصحيح
import com.example.applicationapp.repository.ProductRepository
import kotlinx.coroutines.launch

@Composable
fun ProductDetailsScreen(productId: String?, repository: ProductRepository) {
    val scope = rememberCoroutineScope()
    var priceList by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        scope.launch {
            if (!productId.isNullOrEmpty()) {
                val product = repository.getProductById(productId)
                product?.let {
                    priceList = listOf(
                        "متجر A" to it.price,
                        "متجر B" to it.price * 1.05,
                        "متجر C" to it.price * 0.95
                    )
                }
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "مقارنة الأسعار", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) // ✅ إصلاح الخطأ هنا
        } else {
            priceList.forEach { (store, price) ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = store, style = MaterialTheme.typography.headlineSmall)
                        Text(text = "السعر: ${price} ريال")
                    }
                }
            }
        }
    }
}

