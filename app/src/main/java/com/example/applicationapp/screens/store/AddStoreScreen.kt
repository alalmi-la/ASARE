package com.example.applicationapp.screens

import android.location.Geocoder
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
import java.util.Locale

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
    var storeAddress by remember { mutableStateOf("") } // ✅ العنوان التلقائي

    val navEntry = remember(navController) { navController.currentBackStackEntry }
    val savedStateHandle = navEntry?.savedStateHandle

    val latFromMap by savedStateHandle?.getLiveData<Double>("selected_store_lat")?.observeAsState()
        ?: remember { mutableStateOf(null) }
    val lngFromMap by savedStateHandle?.getLiveData<Double>("selected_store_lng")?.observeAsState()
        ?: remember { mutableStateOf(null) }

    // ✅ جلب العنوان عند اختيار الموقع
    LaunchedEffect(latFromMap, lngFromMap) {
        val lat = latFromMap
        val lng = lngFromMap
        if (lat != null && lng != null) {
            val location = GeoPoint(lat, lng)
            selectedLocation = location

            savedStateHandle?.remove<Double>("selected_store_lat")
            savedStateHandle?.remove<Double>("selected_store_lng")

            viewModel.getAddressFromLocation(context, location) { address ->
                storeAddress = address
            }
        }
    }


    AppTheme {
        Scaffold(
            topBar = {
                TopBarWithLogo(
                    title = "🛒 إضافة متجر",
                    showBack = true,
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        when {
                            storeName.text.isBlank() ->
                                Toast.makeText(context, "يرجى إدخال اسم المتجر", Toast.LENGTH_SHORT).show()
                            selectedLocation == null ->
                                Toast.makeText(context, "يرجى اختيار موقع من الخريطة", Toast.LENGTH_SHORT).show()
                            else -> {
                                viewModel.checkAndAddStore(
                                    name = storeName.text.trim(),
                                    location = selectedLocation!!,
                                    address = storeAddress
                                ) { exists ->
                                    if (exists) {
                                        Toast.makeText(context, "المتجر موجود مسبقًا", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "🎉 تمت إضافة المتجر!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("💾 حفظ المتجر")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المتجر") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        navController.navigate("store_map?mode=select")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("اختيار الموقع من الخريطة")
                }

                OutlinedTextField(
                    value = selectedLocation?.let {
                        "📍 ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}"
                    } ?: "",
                    onValueChange = {},
                    label = { Text("الإحداثيات") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeAddress,
                    onValueChange = {},
                    label = { Text("📍 عنوان المتجر") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

