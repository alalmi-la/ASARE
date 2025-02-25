package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.ui.theme.AccentColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparePricesScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String
) {
    // العثور على المنتج الأساسي من خلال معرّفه
    val products by viewModel.productList.collectAsState(initial = emptyList())
    val baseProduct = products.find { it.id == productId }
    // تصفية المنتجات باستخدام الباركود
    val filteredProducts = baseProduct?.let { product ->
        products.filter { it.barcode == product.barcode }
    } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مقارنة الأسعار", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (filteredProducts.isEmpty()) {
                Text(
                    text = "لا توجد بيانات لهذا المنتج في متاجر أخرى.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredProducts) { product ->
                        ComparePriceItem(product = product)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Text("رجوع", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ComparePriceItem(product: com.example.asare_montagrt.data.model.Product) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.storeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = product.storeLocation,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "السعر: ${product.price} ريال",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

