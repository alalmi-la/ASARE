package com.example.applicationapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product

@Composable
fun ComparePricesScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String
) {
    // استرجاع قائمة المنتجات من الـ ViewModel
    val products by viewModel.productList.collectAsState(initial = emptyList())
    // تصفية المنتجات بناءً على productId
    val filteredProducts = products.filter { it.id == productId }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "مقارنة الأسعار",
                style = MaterialTheme.typography.headlineMedium
            )

            if (filteredProducts.isEmpty()) {
                Text(
                    text = "لا توجد بيانات لهذا المنتج في متاجر أخرى.",
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn {
                    items(filteredProducts) { product ->
                        ComparePriceItem(product)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("رجوع")
            }
        }
    }
}

@Composable
fun ComparePriceItem(product: Product) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "المتجر: ${product.store}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "السعر: ${product.price} DA", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

