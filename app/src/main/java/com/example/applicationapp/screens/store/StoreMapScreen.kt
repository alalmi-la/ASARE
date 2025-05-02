package com.example.applicationapp.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.model.Store
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StoreMapScreen(
    navController: NavController,
    fromDetails: Boolean = false,
    targetStore: Store? = null,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val stores by viewModel.storeList.collectAsState(initial = emptyList())

    var pickedLocation by remember { mutableStateOf<LatLng?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    PricesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Map", color = PricesTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PricesTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
            floatingActionButton = {
                if (!fromDetails && pickedLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            pickedLocation?.let {
                                viewModel.setPickedLocation(it.latitude, it.longitude)
                                navController.popBackStack()
                            }
                        },
                        containerColor = PricesSelectedIconColor
                    ) {
                        Text("✓", color = Color.White)
                    }
                }
            },
            containerColor = PricesBackgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        properties = MapProperties(isMyLocationEnabled = true),
                        uiSettings = MapUiSettings(zoomControlsEnabled = true),
                        onMapClick = { latLng ->
                            if (!fromDetails) {
                                pickedLocation = latLng
                            }
                        }
                    ) {
                        stores.forEach { store ->
                            Marker(
                                state = MarkerState(position = LatLng(store.latitude, store.longitude)),
                                title = store.name
                            )
                        }

                        if (fromDetails && targetStore != null && userLocation != null) {
                            Polyline(
                                points = listOf(
                                    userLocation!!,
                                    LatLng(targetStore.latitude, targetStore.longitude)
                                ),
                                color = PricesSelectedIconColor,
                                width = 5f
                            )
                        }

                        userLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Your Location"
                            )
                        }
                        targetStore?.let {
                            Marker(
                                state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                                title = it.name
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Allow location access to see the map", color = PricesTextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = PricesSelectedIconColor)
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.getCurrentUserLocation(context)
            viewModel.getUserLocation()?.let {
                userLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }
}
