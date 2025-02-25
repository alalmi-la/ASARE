package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.applicationapp.components.ProductItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(navController: NavController, viewModel: ProductViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val products by viewModel.productList.collectAsState(initial = emptyList())
    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قائمة المنتجات", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { navController.navigate("addProduct") },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { navController.navigate("barcodeScanner") },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Text("📷", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث عن منتج...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions(onSearch = { /* تنفيذ البحث */ })
            )

            LazyColumn {
                items(filteredProducts) { product ->
                    ProductItem(product = product, navController = navController)
                }
            }
        }
    }
}
