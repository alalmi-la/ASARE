package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.components.*
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.SortType
import com.example.applicationapp.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sortOptions = SortType.values().toList()
    val isLoading = viewModel.isLoading.collectAsState()
    val suggestions = viewModel.searchSuggestions.collectAsState()
    val currentSortType = viewModel.currentSortType.collectAsState()

    // تحميل البيانات عند الدخول لأول مرة
    LaunchedEffect(Unit) {
        if (viewModel.productList.value.isEmpty()) viewModel.loadProducts()
        if (viewModel.storeList.value.isEmpty()) viewModel.loadStores()
        viewModel.getCurrentUserLocation(context)
    }

    // نتائج البحث أو الترتيب
    val products = if (searchQuery.isNotBlank()) {
        viewModel.getProductsByName(searchQuery)
    } else {
        viewModel.getSortedProducts()
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Prices", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = { /* Drawer قادم */ }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                text = { Text("الحساب الشخصي") },
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
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                text = { Text("الإشعارات") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("notifications")
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = { BottomNavigationBar(navController) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_product") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة منتج") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // شريط البحث
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showSuggestions = it.isNotBlank()
                        if (it.isNotBlank()) viewModel.fetchAutocompleteSuggestions(it)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = { Text("ابحث عن منتج...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                // اقتراحات البحث التلقائي
                if (showSuggestions && suggestions.value.isNotEmpty()) {
                    suggestions.value.take(5).forEach { suggestion ->
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

                // قائمة الترتيب
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }
                ) {
                    TextField(
                        value = currentSortType.value.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ترتيب حسب") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    viewModel.updateSortType(option)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                // العنوان حسب الحالة
                val title = when {
                    searchQuery.isNotBlank() -> "نتائج البحث"
                    else -> when (currentSortType.value) {
                        SortType.TOP_RATED -> "الأعلى تقييمًا"
                        SortType.FEATURED -> "منتجات بارزة"
                        SortType.NEARBY -> "الأقرب إليك"
                        SortType.ALL -> "كل المنتجات"
                    }
                }
                Text(title, style = MaterialTheme.typography.titleLarge)

                // عرض المنتجات أو حالة التحميل
                if (isLoading.value) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (products.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp)
                        ) {
                            Text("لا توجد نتائج", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(products) { product ->
                                ProductCardTopRated(
                                    product = product,
                                    onClick = {
                                        val key = product.barcode.ifBlank { product.name }
                                        viewModel.recordProductSearch(product)
                                        navController.navigate("price_list/$key")
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp)) // المساحة السفلى لـ FAB
            }
        }
    }
}

