package com.example.applicationapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.applicationapp.repository.ProductRepository
import com.example.applicationapp.viewmodel.ProductViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    repository: ProductRepository,
    modifier: Modifier = Modifier
) {
    val productViewModel: ProductViewModel = hiltViewModel()
    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            composable("splash") { SplashScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("home") { ProductListScreen(navController, productViewModel) }

            // شاشة إضافة المنتج مع إمكانية استقبال معطيات barcode/storeName/storeLocation
            composable(
                route = "addProduct?barcode={barcode}&storeName={storeName}&storeLocation={storeLocation}",
                arguments = listOf(
                    navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                    navArgument("storeName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("storeLocation") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                val storeName = backStackEntry.arguments?.getString("storeName") ?: ""
                val storeLocation = backStackEntry.arguments?.getString("storeLocation") ?: ""
                AddProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    productId = null,
                    isUpdateMode = false,
                    initialBarcode = barcode,
                    initialStoreName = storeName,
                    initialStoreLocation = storeLocation
                )
            }
            composable("barcodeScanner") { BarcodeScannerScreen(navController, repository) }
            composable(
                route = "productDetails/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailsScreen(navController, productViewModel, productId)
            }
            composable(
                route = "compare/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ComparePricesScreen(navController, productViewModel, productId)
            }
            composable("forgotPassword") { ForgotPasswordScreen(navController) }
            // مسار تحديث المنتج باستخدام شاشة إضافة المنتج بوضع التحديث
            composable(
                route = "updateProduct/{productId}",
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
        }
    }
}
