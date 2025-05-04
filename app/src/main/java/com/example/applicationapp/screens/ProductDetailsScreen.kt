package com.example.applicationapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
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

    val product = remember(productId, scannedBarcode) {
        productId
            ?.let { viewModel.getProductByIdNow(it) }
            ?: scannedBarcode
                ?.let { viewModel.getProductsByBarcodeNow(it).firstOrNull() }
    }

    val user = viewModel.currentUser.collectAsState().value

    var averageRating by remember { mutableStateOf<Float?>(null) }
    var userRating by remember { mutableFloatStateOf(0f) }
    var priceHistory by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }

    LaunchedEffect(product) {
        product?.let {
            viewModel.recordProductSearch(it)
            averageRating = viewModel.getAverageRating(it.barcode, it.storeName)
            userRating = viewModel.getUserRating(it.barcode, it.storeName, user?.id ?: "") ?: 0f
            priceHistory = viewModel.getPriceHistory(it.barcode, it.storeName)
        }
    }

    val isGeneral = productId == null && scannedBarcode != null

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = product?.name ?: "تفاصيل المنتج",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                if (product != null) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(
                                "add_product_screen" +
                                        "?name=${product.name}" +
                                        "&barcode=${product.barcode}" +
                                        "&imageUrl=${product.imageUrl}" +
                                        "&storeName=${product.storeName}" +
                                        "&location=${product.storeLocation}" +
                                        "&productId=${product.id}" +
                                        "&isUpdateMode=${!isGeneral}"
                            )
                        },
                        containerColor = PrimaryColor
                    ) {
                        Text("+", color = OnPrimaryColor)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (product == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Text(text = product.name, style = MaterialTheme.typography.headlineLarge)
                    Text(text = "📦 الباركود: ${product.barcode}", style = MaterialTheme.typography.bodyMedium)

                    Button(
                        onClick = { navController.navigate("price_list/${product.barcode}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("قارن الأسعار", color = OnPrimaryColor)
                    }

                    if (!isGeneral) {
                        if (product.storeName.isNotEmpty()) {
                            Text("🏬 المتجر: ${product.storeName}", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (product.storeLocation != null) {
                            Text(
                                "🌍 موقع المتجر (إحداثيات): ${product.storeLocation.latitude}, ${product.storeLocation.longitude}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        val prices = viewModel.getProductsByBarcodeNow(product.barcode).map { it.price }
                        if (prices.isNotEmpty()) {
                            Text("📉 أقل سعر: ${prices.minOrNull()} DZD", color = VerifiedGreen)
                            Text("📈 أعلى سعر: ${prices.maxOrNull()} DZD", color = DiscountRed)
                            Text(
                                "⚖️ متوسط السعر: ${"%.2f".format(prices.average())} DZD",
                                color = WarningOrange
                            )
                        }

                        Column {
                            Text("⭐ تقييم السعر", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = userRating,
                                onValueChange = { userRating = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        val success = viewModel.submitPriceRating(
                                            barcode = product.barcode,
                                            storeName = product.storeName,
                                            rating = userRating
                                        )
                                        if (success) {
                                            averageRating = viewModel.getAverageRating(
                                                product.barcode,
                                                product.storeName
                                            )
                                        }
                                    }
                                },
                                valueRange = 0f..5f,
                                steps = 4,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryColor,
                                    activeTrackColor = PrimaryColor,
                                    inactiveTrackColor = DividerGray
                                )
                            )
                            averageRating?.let {
                                Text(
                                    "متوسط التقييم: ${"%.1f".format(it)} / 5",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (priceHistory.isNotEmpty()) {
                            Text("📊 تغير السعر مع الوقت", style = MaterialTheme.typography.titleMedium)
                            val chartEntries = priceHistory.mapIndexed { index, pair ->
                                FloatEntry(index.toFloat(), pair.second.toFloat())
                            }
                            val chartModelProducer = ChartEntryModelProducer(listOf(chartEntries))
                            Text("الرسم البياني هنا (LineChart معطل)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

