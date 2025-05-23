package com.example.applicationapp.screens

import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.R
import com.example.applicationapp.components.TopBarWithLogo
import com.example.applicationapp.model.Store
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product
import com.google.firebase.auth.FirebaseAuth
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

    val product by produceState<Product?>(initialValue = null, productId, scannedBarcode) {
        value = when {
            productId != null -> viewModel.getProductById(productId)
            scannedBarcode != null -> viewModel.getProductsByBarcodeNow(scannedBarcode).firstOrNull()
            else -> null
        }
    }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val p = product!!
    val isGeneral = productId == null && scannedBarcode != null

    var showRatingDialog by remember { mutableStateOf(false) }
    val priceHistories by viewModel.priceHistories.collectAsState()
    val lastHistory = viewModel.getLastPriceHistory(p)

    var averageRating by remember { mutableStateOf(0f) }
    var ratingsCount by remember { mutableStateOf(0) }
    var userRating by remember { mutableStateOf(0f) }

    // قائمة المتاجر المحمّلة
    val stores by viewModel.storeList.collectAsState(initial = emptyList<Store>())
    var storeAddress by remember { mutableStateOf("") }

    LaunchedEffect(priceHistories) {
        lastHistory?.let {
            averageRating = it.averageRating.toFloat()
            ratingsCount = it.ratingsCount
            userRating = viewModel.getUserRating(p.barcode, p.storeName, user?.id ?: "") ?: 0f
        }
    }

    LaunchedEffect(p, isGeneral) {
        if (!isGeneral) {
            averageRating = viewModel.getAverageRating(p.barcode, p.storeName)
            userRating = viewModel.getUserRating(p.barcode, p.storeName, user?.id ?: "") ?: 0f
        }
    }

    // جلب عنوان المتجر: أولاً من قائمة المتاجر، ثم من Geocoder إذا لم يوجد
    LaunchedEffect(p.storeName, p.storeLocation, stores) {
        val match = stores.find { s ->
            s.name == p.storeName &&
                    s.latitude == p.storeLocation?.latitude &&
                    s.longitude == p.storeLocation?.longitude
        }
        if (match != null && match.address.isNotBlank()) {
            storeAddress = match.address
        } else {
            p.storeLocation?.let { loc ->
                viewModel.getAddressFromLocation(context, loc) { result ->
                    storeAddress = result
                }
            }
        }
    }

    val prices = viewModel.getProductsByBarcodeNow(p.barcode).map { it.price }
    val hasPrices = prices.isNotEmpty()
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val avgPrice = if (hasPrices) prices.average() else 0.0

    AppTheme {
        Scaffold(
            topBar = {
                TopBarWithLogo(
                    title = p.name,
                    showBack = true,
                    onBackClick = { navController.popBackStack() }
                )
            },
            floatingActionButton = {
                IconButton(
                    onClick = {
                        navController.navigate("add_product") {
                            launchSingleTop = true
                        }
                        navController.getBackStackEntry("add_product")
                            .savedStateHandle
                            .set("edit_product_id", p.id)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
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
                            model = p.imageUrl,
                            placeholder = painterResource(id = R.drawable.placeholder_image)
                        ),
                        contentDescription = "صورة المنتج",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .aspectRatio(1.6f),
                        contentScale = ContentScale.Fit
                    )
                }
                item {
                    Text(p.name, style = MaterialTheme.typography.headlineMedium)
                    Text("📦 ${p.barcode}", style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    Button(
                        onClick = { navController.navigate("price_list/${p.barcode}") },
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
                        Text("⚖️ متوسط السعر: ${"%.2f".format(avgPrice)} د.ج",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!isGeneral) {
                    item {
                        Text(
                            "💰 السعر الحالي: ${p.price} د.ج",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    item { Text("🏬 المتجر: ${p.storeName}", style = MaterialTheme.typography.bodyMedium) }
                    item {
                        if (storeAddress.isNotBlank()) {
                            Text("📍 عنوان المتجر: $storeAddress",
                                style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("📍 جاري تحميل عنوان المتجر...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray)
                        }
                    }
                    item {
                        Text("⭐ تقييم السعر:", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (lastHistory != null) {
                                repeat(5) { i ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i < averageRating.toInt()) Color(0xFFFFC107) else Color.Gray
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("(${ratingsCount} تقييمات)",
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("⭐ لا يوجد تقييم بعد",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showRatingDialog = true }) {
                                Text("قيّم", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        if (showRatingDialog) {
                            RatingDialogM2(
                                currentRating = averageRating,
                                onDismiss = { showRatingDialog = false },
                                onSubmit = { selectedRating ->
                                    showRatingDialog = false
                                    scope.launch {
                                        try {
                                            val uid = FirebaseAuth.getInstance().uid
                                            if (uid.isNullOrEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "خطأ: المستخدم غير مسجل الدخول",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }
                                            viewModel.rateProduct(p, selectedRating)
                                        } catch (e: Exception) {
                                            Log.e("RATE_ERROR", "فشل عند إرسال التقييم", e)
                                            Toast.makeText(
                                                context,
                                                "حدث خطأ أثناء إرسال التقييم",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDialogM2(
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
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < selected.toInt()) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
