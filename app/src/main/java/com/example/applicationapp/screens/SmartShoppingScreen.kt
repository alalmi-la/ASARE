package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.components.ProductItemCard
import com.example.applicationapp.components.StoreResultCard
import com.example.applicationapp.components.TopBarWithLogo
import com.example.applicationapp.model.Store
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.applicationapp.viewmodel.SortType
import com.example.asare_montagrt.data.model.Product
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartShoppingScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val liveSearchResults = remember { mutableStateOf<List<Product>>(emptyList()) }
    var cheapestStores by remember { mutableStateOf<List<Pair<Store, Double>>>(emptyList()) }

    val selectedProducts by viewModel.selectedProducts.collectAsState()
    val products = viewModel.productList.collectAsState().value
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (products.isEmpty()) viewModel.loadProducts()
        if (viewModel.storeList.value.isEmpty()) viewModel.loadStores()
        viewModel.getCurrentUserLocation(context)

        if (searchQuery.isBlank() && liveSearchResults.value.isEmpty()) {
            liveSearchResults.value = products
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastIndex ->
                if (lastIndex == products.lastIndex && !isLoading) {
                    viewModel.loadMoreProducts()
                }
            }
    }

    val filteredProducts = liveSearchResults.value
        .groupBy { it.barcode }
        .mapNotNull { it.value.minByOrNull { p -> p.price } }

    AppTheme {
        Scaffold(
            topBar = {
                TopBarWithLogo(
                    title = "التسوق الذكي",
                    showBack = true,
                    onBackClick = { navController.popBackStack() },

                    )

            },
            bottomBar = { BottomNavigationBar(navController) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.analyzeCheapestStoresForSelectedProducts {
                            cheapestStores = it
                        }
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    text = { Text("أرخص متجر") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 56.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        cheapestStores = emptyList()

                        if (it.isNotBlank()) {
                            viewModel.searchProductsLive(it) { results ->
                                liveSearchResults.value = results
                            }
                        } else {
                            liveSearchResults.value = products
                        }
                    },
                    placeholder = { Text("ابحث عن منتج...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                navController.navigate("barcode_scanner?source=SMART_SHOPPING")
                            }
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "ماسح")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    cheapestStores.isNotEmpty() -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cheapestStores) { (store, total) ->
                                StoreResultCard(
                                    store = store,
                                    total = total,
                                    onNavigateClick = {
                                        navController.navigate("store_map?mode=route&lat=${store.latitude}&lng=${store.longitude}")
                                    }
                                )
                            }
                        }
                    }

                    filteredProducts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد منتجات حالياً",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredProducts) { product ->
                                val isSelected = selectedProducts.any { it.barcode == product.barcode }

                                ProductItemCard(
                                    product = product,
                                    isSelected = isSelected,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) viewModel.addSelectedProduct(product)
                                        else viewModel.removeSelectedProduct(product)
                                    }
                                )
                            }

                            if (isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
