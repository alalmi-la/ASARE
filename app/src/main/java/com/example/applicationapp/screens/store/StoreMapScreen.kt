package com.example.applicationapp.screens.store

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.model.Store
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreMapScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val stores by viewModel.storeList.collectAsState()
    val context = LocalContext.current
    var selectedStore by remember { mutableStateOf<Store?>(null) }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("اختيار المتجر", color = PricesTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = PricesTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
            floatingActionButton = {
                selectedStore?.let { store ->
                    ExtendedFloatingActionButton(
                        onClick = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("selected_store_name", store.name)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("selected_store_location", GeoPoint(store.latitude, store.longitude))
                            navController.popBackStack()
                        },
                        containerColor = PricesSelectedIconColor
                    ) {
                        Text("اختيار ${store.name}", color = Color.White)
                    }
                }
            },
            containerColor = PricesBackgroundColor
        ) { padding ->
            val cameraPositionState = rememberCameraPositionState()

            GoogleMap(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                cameraPositionState = cameraPositionState
            ) {
                stores.forEach { store ->
                    val position = LatLng(store.latitude, store.longitude)
                    val markerState = rememberMarkerState(position = position)
                    Marker(
                        state = markerState,
                        title = store.name,
                        snippet = "اضغط لتحديد هذا المتجر",
                        onClick = {
                            selectedStore = store
                            markerState.showInfoWindow()
                            true
                        }
                    )
                }
            }
        }
    }
}
