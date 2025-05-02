package com.example.applicationapp.screens

import android.location.Location
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
    productId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val product = viewModel.getProductByIdNow(productId)
    val user = viewModel.currentUser.collectAsState().value

    var averageRating by remember { mutableStateOf<Float?>(null) }
    var userRating by remember { mutableFloatStateOf(0f) }
    var priceHistory by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }

    LaunchedEffect(Unit) {
        product?.let {
            viewModel.recordProductSearch(it)
            averageRating = viewModel.getAverageRating(it.barcode, it.storeName)
            userRating = viewModel.getUserRating(it.barcode, it.storeName, user?.id ?: "") ?: 0f
            priceHistory = viewModel.getPriceHistory(it.barcode, it.storeName)
        }
    }

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
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (product == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    // صورة المنتج
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // التفاصيل
                    Text(text = product.name, style = MaterialTheme.typography.headlineLarge)
                    Text(text = "📦 الباركود: ${product.barcode}", style = MaterialTheme.typography.bodyMedium)
                    if (product.storeName.isNotEmpty()) {
                        Text("🏬 المتجر: ${product.storeName}", style = MaterialTheme.typography.bodyMedium)
                    }

                    // تحليل الأسعار
                    val prices = viewModel.getProductsByBarcodeNow(product.barcode).map { it.price }
                    if (prices.isNotEmpty()) {
                        Text("📉 أقل سعر: ${prices.minOrNull()} DZD", color = VerifiedGreen)
                        Text("📈 أعلى سعر: ${prices.maxOrNull()} DZD", color = DiscountRed)
                        Text("⚖️ متوسط السعر: ${"%.2f".format(prices.average())} DZD", color = WarningOrange)
                    }

                    // زر المقارنة
                    Button(
                        onClick = { navController.navigate("price_list/${product.barcode}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("قارن الأسعار", color = OnPrimaryColor)
                    }

                    // التقييم
                    Column {
                        Text("⭐ تقييم السعر", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = userRating,
                            onValueChange = { userRating = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    viewModel.submitPriceRating(
                                        barcode = product.barcode,
                                        storeName = product.storeName,
                                        rating = userRating
                                    ) { success ->
                                        if (success) {
                                            scope.launch {
                                                averageRating = viewModel.getAverageRating(product.barcode, product.storeName)
                                            }
                                        }
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
                            Text("متوسط التقييم: ${"%.1f".format(it)} / 5", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // الرسم البياني للسعر
                    if (priceHistory.isNotEmpty()) {
                        Text("📊 تغير السعر مع الوقت", style = MaterialTheme.typography.titleMedium)

                        val chartEntries = priceHistory.mapIndexed { index, pair ->
                            FloatEntry(index.toFloat(), pair.second.toFloat())
                        }

                        val chartModelProducer = ChartEntryModelProducer(listOf(chartEntries))

                        Text("الرسم البياني هنا (LineChart معطل)", style = MaterialTheme.typography.bodySmall)

                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
