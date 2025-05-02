package com.example.applicationapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStoreScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    var storeName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val locationFromMap = savedStateHandle
        ?.getLiveData<GeoPoint>("selectedStoreLocation")
        ?.observeAsState()

    LaunchedEffect(locationFromMap?.value) {
        locationFromMap?.value?.let {
            selectedLocation = it
            savedStateHandle?.remove<GeoPoint>("selectedStoreLocation")
        }
    }

    PricesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Store", color = PricesTextPrimary) },
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
            containerColor = PricesBackgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PricesSelectedIconColor,
                        unfocusedBorderColor = PricesTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate("store_map")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PricesSelectedIconColor)
                ) {
                    Text(
                        if (selectedLocation != null) "✅ Location Selected"
                        else "Select Location from Map",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (storeName.text.isBlank()) {
                            Toast.makeText(context, "Please enter store name", Toast.LENGTH_SHORT).show()
                        } else if (selectedLocation == null) {
                            Toast.makeText(context, "Please select a location from the map", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.checkAndAddStore(storeName.text.trim(), selectedLocation!!) { exists ->
                                if (exists) {
                                    Toast.makeText(context, "Store already exists!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Store added successfully!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PricesSelectedIconColor)
                ) {
                    Text("Save Store", color = Color.White)
                }
            }
        }
    }
}
