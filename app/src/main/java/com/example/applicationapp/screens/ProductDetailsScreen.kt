package com.example.applicationapp.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.R
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String? = null,
    scannedBarcode: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = viewModel.currentUser.collectAsState().value

    val product = remember(productId, scannedBarcode) {
        productId?.let { viewModel.getProductByIdNow(it) }
            ?: scannedBarcode?.let { viewModel.getProductsByBarcodeNow(it).firstOrNull() }
    } ?: return

    val isGeneral = productId == null && scannedBarcode != null

    var averageRating by remember { mutableStateOf(0f) }
    var userRating by remember { mutableStateOf(0f) }
    var showRatingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(product, isGeneral) {
        if (!isGeneral) {
            averageRating = viewModel.getAverageRating(product.barcode, product.storeName)
            userRating = viewModel.getUserRating(
                product.barcode,
                product.storeName,
                user?.id ?: ""
            ) ?: 0f
        }
    }

    val prices = viewModel.getProductsByBarcodeNow(product.barcode).map { it.price }
    val hasPrices = prices.isNotEmpty()
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val avgPrice = if (hasPrices) prices.average() else 0.0

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(product.name, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                IconButton(
                    onClick = {
                        navController.navigate(
                            "add_product_screen" +
                                    "?name=${product.name}" +
                                    "&barcode=${product.barcode}" +
                                    "&imageUrl=${product.imageUrl}" +
                                    "&storeName=${product.storeName}" +
                                    "&lat=${product.storeLocation?.latitude ?: 0f}" +
                                    "&lng=${product.storeLocation?.longitude ?: 0f}" +
                                    "&productId=${product.id}" +
                                    "&isUpdateMode=${!isGeneral}"
                        )
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryColor)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = product.imageUrl,
                            placeholder = painterResource(id = R.drawable.placeholder_image)
                        ),
                        contentDescription = "صورة المنتج",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                item {
                    Text(product.name, style = MaterialTheme.typography.headlineMedium)
                    Text("📦 ${product.barcode}", style = MaterialTheme.typography.bodyMedium)
                }

                item {
                    Button(
                        onClick = { navController.navigate("price_list/${product.barcode}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("قارن الأسعار", color = OnPrimaryColor)
                    }
                }

                if (isGeneral && hasPrices) {
                    item { Text("📉 أقل سعر: $minPrice د.ج", style = MaterialTheme.typography.bodyMedium) }
                    item { Text("📈 أعلى سعر: $maxPrice د.ج", style = MaterialTheme.typography.bodyMedium) }
                    item {
                        Text(
                            "⚖️ متوسط السعر: ${"%.2f".format(avgPrice)} د.ج",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!isGeneral) {
                    item { Text("🏬 المتجر: ${product.storeName}", style = MaterialTheme.typography.bodyMedium) }
                    product.storeLocation?.let { loc ->
                        item {
                            Text(
                                "🌍 الموقع: خط العرض ${loc.latitude}، خط الطول ${loc.longitude}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item {
                        Text("⭐ تقييم السعر:", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i < averageRating.toInt()) Color(0xFFFFC107) else Color.Gray
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showRatingDialog = true }) {
                                Text("قيّم", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (showRatingDialog) {
                RatingDialog(
                    currentRating = userRating,
                    onDismiss = { showRatingDialog = false },
                    onSubmit = { selected ->
                        showRatingDialog = false
                        scope.launch {
                            val success = viewModel.submitPriceRating(
                                barcode = product.barcode,
                                storeName = product.storeName,
                                rating = selected
                            )
                            Toast.makeText(
                                context,
                                if (success) "تم حفظ التقييم" else "فشل في حفظ التقييم",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (success) {
                                averageRating =
                                    viewModel.getAverageRating(product.barcode, product.storeName)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RatingDialog(
    currentRating: Float,
    onDismiss: () -> Unit,
    onSubmit: (Float) -> Unit
) {
    var selected by remember { mutableStateOf(currentRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قيّم السعر") },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { i ->
                    IconButton(onClick = { selected = (i + 1).toFloat() }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < selected.toInt()) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
