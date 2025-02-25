package com.example.applicationapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.ui.theme.AccentColor
import com.example.applicationapp.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String
) {
    val products by viewModel.productList.collectAsState(initial = emptyList())
    val product = products.find { it.id == productId }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا المنتج؟") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.deleteProduct(productId)
                        showDeleteDialog = false
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }) { Text("نعم") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل المنتج", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            product?.let {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(it.imageUrl),
                            contentDescription = "صورة المنتج",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "السعر: ${it.price} ريال",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "المتجر: ${it.storeName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "الموقع: ${it.storeLocation}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("updateProduct/${it.id}") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تحديث السعر", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("compare/${it.id}") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مقارنة الأسعار", color = MaterialTheme.colorScheme.onSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حذف المنتج", color = MaterialTheme.colorScheme.onError)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("رجوع", color = MaterialTheme.colorScheme.onPrimary)
                }
            } ?: run {
                Text("لم يتم العثور على المنتج.", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
