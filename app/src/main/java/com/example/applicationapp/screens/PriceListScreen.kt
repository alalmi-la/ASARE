package com.example.applicationapp.screens

import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    navController: NavController,
    barcode: String,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    var sortOption by remember { mutableStateOf("الأرخص") }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        if (viewModel.productList.value.isEmpty()) viewModel.loadProducts()
        if (viewModel.storeList.value.isEmpty()) viewModel.loadStores()
        viewModel.getCurrentUserLocation(context)
        userLocation = viewModel.getUserLocation()

    }

    val allProducts = if (barcode.isNotBlank()) {
        viewModel.getProductsByBarcodeNow(barcode)
    } else viewModel.getProductsByName(barcode)

    val sortedProducts = viewModel.getProductsSorted(allProducts, sortOption, userLocation)
    val firstProduct = sortedProducts.firstOrNull()

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            firstProduct?.imageUrl?.let { imageUrl ->
                                Image(
                                    painter = rememberAsyncImagePainter(imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("أسعار المنتج", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "فرز")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("الأرخص") }, onClick = {
                                sortOption = "الأرخص"; expanded = false
                            })
                            DropdownMenuItem(text = { Text("الأقرب") }, onClick = {
                                sortOption = "الأقرب"; expanded = false
                            })
                            DropdownMenuItem(text = { Text("الأحدث") }, onClick = {
                                sortOption = "الأحدث"; expanded = false
                            })
                        }
                    }
                )
            }
        ) { padding ->
            if (sortedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد نتائج", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(padding)
                ) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            firstProduct?.imageUrl?.let { imageUrl ->
                                Image(
                                    painter = rememberAsyncImagePainter(imageUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(MaterialTheme.shapes.large)
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(firstProduct?.name ?: "اسم المنتج", style = MaterialTheme.typography.titleLarge)
                            Text("عدد النتائج: ${sortedProducts.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    items(sortedProducts) { product ->
                        ElevatedCard(
                            onClick = { navController.navigate("product_details/${product.id}") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(product.storeName, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("${product.price} DZD", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "آخر تحديث: ${formatDate(product.updatedAt)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                viewModel.getStoreDistanceInKm(userLocation, product.storeLocation)?.let { km ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "المسافة التقريبية: ${"%.1f".format(km)} كم",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
