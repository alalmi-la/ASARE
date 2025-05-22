package com.example.applicationapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.components.TopBarWithLogo
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStoreScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current

    var storeName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val navEntry = remember(navController) { navController.currentBackStackEntry }
    val savedStateHandle = navEntry?.savedStateHandle

    val latFromMap by savedStateHandle?.getLiveData<Double>("selected_store_lat")?.observeAsState()
        ?: remember { mutableStateOf(null) }
    val lngFromMap by savedStateHandle?.getLiveData<Double>("selected_store_lng")?.observeAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(latFromMap, lngFromMap) {
        val lat = latFromMap
        val lng = lngFromMap
        if (lat != null && lng != null) {
            selectedLocation = GeoPoint(lat, lng)
            savedStateHandle?.remove<Double>("selected_store_lat")
            savedStateHandle?.remove<Double>("selected_store_lng")
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopBarWithLogo(
                    title = "إضافة متجر",
                    showBack = true,
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = { BottomNavigationBar(navController) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المتجر") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate("store_map?mode=select")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("اختيار الموقع من الخريطة")
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = selectedLocation?.let {
                        "📍 ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}"
                    } ?: "",
                    onValueChange = {},
                    label = { Text("الموقع المختار") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        when {
                            storeName.text.isBlank() ->
                                Toast.makeText(context, "يرجى إدخال اسم المتجر", Toast.LENGTH_SHORT).show()
                            selectedLocation == null ->
                                Toast.makeText(context, "يرجى اختيار موقع من الخريطة", Toast.LENGTH_SHORT).show()
                            else -> {
                                viewModel.checkAndAddStore(
                                    storeName.text.trim(),
                                    selectedLocation!!
                                ) { exists ->
                                    if (exists) {
                                        Toast.makeText(context, "المتجر موجود مسبقًا", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تمت إضافة المتجر بنجاح", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("حفظ المتجر")
                }
            }
        }
    }
}
