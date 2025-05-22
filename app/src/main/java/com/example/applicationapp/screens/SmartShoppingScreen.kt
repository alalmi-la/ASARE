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
    var showFilterMenu by remember { mutableStateOf(false) }
    val liveSearchResults = remember { mutableStateOf<List<Product>>(emptyList()) }
    var cheapestStores by remember { mutableStateOf<List<Pair<Store, Double>>>(emptyList()) }

    val selectedProducts by viewModel.selectedProducts.collectAsState()
    val products = viewModel.productList.collectAsState().value
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    // تحميل أولي
    LaunchedEffect(Unit) {
        if (products.isEmpty()) viewModel.loadProducts()
        if (viewModel.storeList.value.isEmpty()) viewModel.loadStores()
        viewModel.getCurrentUserLocation(context)

        // عرض أولي للمنتجات بدون بحث
        if (searchQuery.isBlank() && liveSearchResults.value.isEmpty()) {
            liveSearchResults.value = products
        }
    }

    // تحميل تدريجي
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
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
                    actions = {
                        IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                            Icon(Icons.Default.Tune, contentDescription = "فلترة")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            SortType.values().forEach { sortTypeItem ->
                                DropdownMenuItem(
                                    text = { Text(sortTypeItem.displayName) },
                                    onClick = {
                                        showFilterMenu = false
                                        viewModel.updateSortType(sortTypeItem)
                                    }
                                )
                            }
                        }
                    }
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

            Column(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)) {

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
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(store.name, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("الإجمالي: $total DZD", color = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            navController.navigate("store_map?mode=route&lat=${store.latitude}&lng=${store.longitude}")
                                        }) {
                                            Icon(Icons.Default.Place, contentDescription = "الخريطة")
                                        }

                                    }
                                }
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
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp, top = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredProducts) { product ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            product.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${product.price} DZD",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Checkbox(
                                            checked = selectedProducts.any { it.barcode == product.barcode },
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) viewModel.addSelectedProduct(product)
                                                else viewModel.removeSelectedProduct(product)
                                            }
                                        )
                                    }
                                }
                            }

                            if (isLoading) {
                                item(span = { GridItemSpan(2) }) {
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
