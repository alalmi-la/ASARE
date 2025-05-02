package com.example.applicationapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.applicationapp.model.Store
import com.example.applicationapp.screens.*
import com.example.applicationapp.screens.LoginScreen
import com.example.applicationapp.screens.product.AddProductScreen
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.firestore.GeoPoint
import android.content.Context


@Composable
fun StartDestinationGraph(navController: NavHostController) {
    val context = LocalContext.current
    val isLoggedIn = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_logged_in", false)
    }

    NavigationGraph(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    )
}
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String
) {
    val productViewModel: ProductViewModel = hiltViewModel()



    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // Login Screen
            composable("login") {
                LoginScreen(navController = navController, viewModel = productViewModel)

            }

            // Home Screen
            composable("home") {
                HomeScreen(navController = navController, viewModel = productViewModel)

            }

            // Price List Screen
            composable(
                route = "price_list/{barcodeOrName}",
                arguments = listOf(navArgument("barcodeOrName") { type = NavType.StringType })
            ) { backStackEntry ->
                val barcodeOrName = backStackEntry.arguments?.getString("barcodeOrName") ?: ""
                PriceListScreen(
                    navController = navController,
                    barcode = barcodeOrName,
                    viewModel = productViewModel
                )
            }

            // Product Details Screen
            composable(
                route = "product_details/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailsScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    productId = productId
                )
            }

            // Add Product Screen
            composable(
                route = "add_product?barcode={barcode}&storeName={storeName}&name={name}&imageUrl={imageUrl}",
                arguments = listOf(
                    navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                    navArgument("storeName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("imageUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                val storeName = backStackEntry.arguments?.getString("storeName") ?: ""
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""

                AddProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    initialBarcode = barcode,
                    initialStoreName = storeName,
                    initialName = name,
                    initialImageUrl = imageUrl
                )
            }

            // Barcode Scanner Screen
            composable(
                route = "barcode?fromShopping={fromShopping}",
                arguments = listOf(
                    navArgument("fromShopping") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val fromShopping = backStackEntry.arguments?.getBoolean("fromShopping") ?: false
                BarcodeScannerScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    fromShopping = fromShopping
                )
            }

            // Add Store Screen
            composable("add_store") {
                AddStoreScreen(
                    navController = navController,
                    viewModel = productViewModel
                )
            }

            // Store Map Screen ✅ إصلاح تمرير viewModel
            composable(
                route = "store_map?readonly={readonly}&destinationLat={destinationLat}&destinationLng={destinationLng}",
                arguments = listOf(
                    navArgument("readonly") { type = NavType.BoolType; defaultValue = false },
                    navArgument("destinationLat") { type = NavType.StringType; defaultValue = "" },
                    navArgument("destinationLng") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val readonly = backStackEntry.arguments?.getBoolean("readonly") ?: false
                val lat = backStackEntry.arguments?.getString("destinationLat")?.toDoubleOrNull()
                val lng = backStackEntry.arguments?.getString("destinationLng")?.toDoubleOrNull()
                val destination = if (lat != null && lng != null) GeoPoint(lat, lng) else null

                StoreMapScreen(
                    navController = navController,
                    fromDetails = readonly,
                    targetStore = destination?.let {
                        Store(
                            name = "المتجر",
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    },
                    viewModel = productViewModel  // ✅ تم إضافة viewModel هنا
                )
            }

            // Settings Screen

            // Update Product Screen
            composable(
                route = "update_product/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                AddProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    productId = productId,
                    isUpdateMode = true
                )
            }

            // Smart Shopping Screen
            composable("shopping") {
                SmartShoppingScreen(
                    navController = navController,
                    viewModel = productViewModel
                )
            }
        }
    }
}
