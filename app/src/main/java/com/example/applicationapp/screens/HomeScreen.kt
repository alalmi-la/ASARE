package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.components.ProductCardTopRated
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.applicationapp.viewmodel.SortType
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val productsSource = viewModel.productList.collectAsState().value
    val sortType by viewModel.currentSortType.collectAsState()

    LaunchedEffect(Unit) {
        if (productsSource.isEmpty()) viewModel.loadProducts()
        if (viewModel.storeList.value.isEmpty()) viewModel.loadStores()
        viewModel.getCurrentUserLocation(context)
    }

    val products = when {
        searchQuery.isNotBlank() -> viewModel.getProductsByName(searchQuery)
        else -> when (sortType) {
            SortType.ALL        -> productsSource
            SortType.TOP_RATED  -> viewModel.getHomeTopRatedProducts()
            SortType.FEATURED   -> viewModel.getHomeFeaturedProducts()
            SortType.NEARBY     -> viewModel.getHomeNearbyProducts()
        }
    }

    val gridState = rememberLazyGridState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    AppTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("المنتجات", color = MaterialTheme.colorScheme.onBackground) },
                        actions = {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.Menu, contentDescription = "قائمة الخيارات")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                    text = { Text("الإشعارات") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate("notifications")
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                    text = { Text("الحساب") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate("account")
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    text = { Text("الإعدادات") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate("settings")
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                showSuggestions = it.isNotBlank()
                                if (it.isNotBlank()) viewModel.fetchAutocompleteSuggestions(it)
                            },
                            placeholder = { Text("ابحث عن منتج...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Box {
                                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                                        Icon(Icons.Default.FilterList, contentDescription = "فلترة")
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
                            }
                        )

                        if (showSuggestions && suggestions.isNotEmpty()) {
                            Column {
                                suggestions.take(5).forEach { suggestion ->
                                    TextButton(
                                        onClick = {
                                            searchQuery = suggestion
                                            showSuggestions = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(suggestion)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = { BottomNavigationBar(navController) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_product") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة منتج") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 56.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refreshProducts() }
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 12.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "🔍 نتائج البحث"
                                    else -> "📂 ${viewModel.currentSortType.value.displayName}"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                        }
                    }


                    itemsIndexed(products) { index, product ->
                        val rating = viewModel.getLastPriceRating(product)
                        val count = viewModel.getLastPriceRatingsCount(product)

                        ProductCardTopRated(
                            product = product,
                            averageRating = rating,
                            ratingsCount = count,
                            onClick = { navController.navigate("product_details/${product.id}") }
                        )

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

            LaunchedEffect(gridState) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .distinctUntilChanged()
                    .collect { lastIndex ->
                        if (lastIndex == products.lastIndex && !isLoading) {
                            viewModel.loadMoreProducts()
                        }
                    }
            }
        }
    }
}
