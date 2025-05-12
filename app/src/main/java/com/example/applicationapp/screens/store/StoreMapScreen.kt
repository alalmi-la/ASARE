package com.example.applicationapp.screens.store

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.applicationapp.model.Store
import com.example.applicationapp.ui.theme.PricesBackgroundColor
import com.example.applicationapp.ui.theme.PricesSelectedIconColor
import com.example.applicationapp.ui.theme.PricesTextPrimary
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreMapScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    mode: String = "pick"
) {
    val context = LocalContext.current
    val stores by viewModel.storeList.collectAsState()

    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اختيار المتجر", color = PricesTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = PricesTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
            )
        },
        floatingActionButton = {
            if (mode == "select") {
                ExtendedFloatingActionButton(
                    onClick = {
                        // إذا لم يحدد المستخدم موقعاً، لا نفعل شيئاً
                        if (selectedLocation == null) return@ExtendedFloatingActionButton

                        val prev = navController.previousBackStackEntry
                        if (prev == null) {
                            Toast.makeText(context, "تعذّر العودة للشاشة السابقة", Toast.LENGTH_SHORT).show()
                            return@ExtendedFloatingActionButton
                        }
                        try {
                            prev.savedStateHandle.set("selected_store_lat", selectedLocation!!.latitude)
                            prev.savedStateHandle.set("selected_store_lng", selectedLocation!!.longitude)
                        } catch (e: Exception) {
                            Log.e("StoreMapScreen", "Error saving selected location", e)
                            Toast.makeText(context, "خطأ أثناء حفظ الموقع", Toast.LENGTH_SHORT).show()
                            return@ExtendedFloatingActionButton
                        }
                        navController.popBackStack()
                    },
                    // نغيّر اللون بين مفعل/معطّل
                    containerColor = if (selectedLocation != null)
                        PricesSelectedIconColor
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (selectedLocation != null) "تأكيد الموقع" else "اختر موقعًا أولاً",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else if (mode == "pick" && selectedStore != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val prev = navController.previousBackStackEntry
                        if (prev == null) {
                            Toast.makeText(context, "تعذّر العودة للشاشة السابقة", Toast.LENGTH_SHORT).show()
                            return@ExtendedFloatingActionButton
                        }
                        try {
                            prev.savedStateHandle.set("selected_store_name", selectedStore!!.name)
                            prev.savedStateHandle.set("selected_store_lat", selectedStore!!.latitude)
                            prev.savedStateHandle.set("selected_store_lng", selectedStore!!.longitude)
                        } catch (e: Exception) {
                            Log.e("StoreMapScreen", "Error saving selected store", e)
                            Toast.makeText(context, "خطأ أثناء حفظ المتجر", Toast.LENGTH_SHORT).show()
                            return@ExtendedFloatingActionButton
                        }
                        navController.popBackStack()
                    },
                    containerColor = PricesSelectedIconColor
                ) {
                    Text("اختيار ${selectedStore!!.name}", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        containerColor = PricesBackgroundColor
    ) { padding ->
        GoogleMap(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            onMapClick = { latLng ->
                if (mode == "select") {
                    // كل نقرة تحرك المؤشّر الأحمر
                    selectedLocation = latLng
                    selectedStore = null
                }
            }
        ) {
            if (mode == "pick") {
                stores.forEach { store ->
                    val pos = LatLng(store.latitude, store.longitude)
                    Marker(
                        state = rememberMarkerState(position = pos),
                        title = store.name,
                        snippet = "اضغط للاختيار",
                        onClick = {
                            selectedStore = store
                            true
                        }
                    )
                }
            } else {
                selectedLocation?.let { latLng ->
                    Marker(
                        // بشيفرة جديدة كل ريندر ليتحرك المؤشّر فوراً
                        state = MarkerState(position = latLng),
                        title = "موقع جديد",
                        snippet = "اضغط تأكيد",
                        onClick = { true }
                    )
                }
            }
        }
    }
}
