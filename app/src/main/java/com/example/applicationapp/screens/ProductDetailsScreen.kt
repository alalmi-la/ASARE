package com.example.applicationapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.viewmodel.ProductViewModel

@Composable
fun ProductDetailsScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String
) {
    // استرجاع قائمة المنتجات من الـ ViewModel
    val products by viewModel.productList.collectAsState(initial = emptyList())
    // البحث عن المنتج باستخدام productId
    val product = products.find { it.id == productId }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            product?.let {
                Text(text = it.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = rememberAsyncImagePainter(it.imageUrl),
                    contentDescription = "صورة المنتج",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(200.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "السعر: ${it.price} DA", style = MaterialTheme.typography.bodyLarge)
                Text(text = "المتجر: ${it.store}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("رجوع")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // زر الانتقال إلى شاشة مقارنة الأسعار
                Button(onClick = { navController.navigate("compare/$productId") }) {
                    Text("مقارنة الأسعار")
                }
            } ?: run {
                Text("لم يتم العثور على المنتج.", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
