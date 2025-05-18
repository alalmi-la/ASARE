package com.example.applicationapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartShoppingScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val products by viewModel.productList.collectAsState(initial = emptyList())
    val selectedProducts = remember { mutableStateListOf<Product>() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Smart Shopping", color = PricesTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PricesTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
            bottomBar = {
                BottomNavigationBar(navController)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        // TODO: تنفيذ البحث عن أرخص متجر لاحقًا
                    },
                    containerColor = PricesSelectedIconColor
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Find Cheapest Store", tint = Color.White)
                }
            },
            containerColor = PricesBackgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // حقل البحث مع زر باركود داخله
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search for a product...") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                navController.navigate("barcode_scanner?source=SMART_SHOPPING")
                            }
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PricesSelectedIconColor,
                        unfocusedBorderColor = PricesSelectedIconColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredProducts.isEmpty()) {
                    // إذا لا توجد منتجات
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No products found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PricesTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = product.name.ifBlank { "Unnamed Product" },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.Black,
                                                fontSize = 18.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${product.price} DZD",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = PricesSelectedIconColor,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                    Checkbox(
                                        checked = selectedProducts.contains(product),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) selectedProducts.add(product)
                                            else selectedProducts.remove(product)
                                        }
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
