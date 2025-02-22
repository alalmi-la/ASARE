package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applicationapp.repository.ProductRepository
import kotlinx.coroutines.launch

@Composable
fun ComparePricesScreenUI(productId: String?, repository: ProductRepository) { // ✅ تغيير الاسم لمنع التعارض
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "مقارنة الأسعار", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            priceList.forEach { (store, price) ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = store, style = MaterialTheme.typography.headlineSmall)
                        Text(text = "السعر: $price دج")
                    }
                }
            }
        }
    }
}

