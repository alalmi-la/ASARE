package com.example.applicationapp.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.applicationapp.screens.Menu.AccountScreen
import com.example.applicationapp.screens.Menu.NotificationScreen
import com.example.applicationapp.screens.Menu.SettingsScreen
import com.example.applicationapp.screens.store.StoreMapScreen
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.BarcodeSource
import com.example.applicationapp.viewmodel.ProductViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    productViewModel: ProductViewModel
) {
    NavHost(navController = navController, startDestination = "auth_check") {

        composable("auth_check") {
        AuthCheckScreen(
            navController = navController,
            productViewModel = productViewModel
          )
        }


        // تسجيل الدخول
        composable("login") {
            LoginScreen(navController = navController, viewModel = productViewModel)
        }

        // الصفحة الرئيسية
        composable("home") {
            HomeScreen(navController = navController, viewModel = productViewModel)
        }

        // قارئ الباركود مع وسيط source
        composable(
            route = "barcode_scanner?source={source}",
            arguments = listOf(
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = "HOME"
                }
            )
        ) { backStackEntry ->
            val sourceArg = backStackEntry.arguments?.getString("source") ?: "HOME"
            productViewModel.setBarcodeSource(BarcodeSource.valueOf(sourceArg))
            BarcodeScannerScreen(navController = navController, viewModel = productViewModel)
        }

        // التسوق الذكي
        composable("smart_shopping") {
            SmartShoppingScreen(navController = navController, viewModel = productViewModel)
        }

        // إضافة متجر
        composable("add_store") {
            AddStoreScreen(navController = navController, viewModel = productViewModel)
        }

        // إضافة منتج (جديد أو تعديل)
        composable("add_product") {
            AppTheme {
                AddProductScreen(
                    navController = navController,
                    viewModel     = productViewModel
                )
            }
        }

        // قائمة الأسعار حسب الباركود
        composable("price_list/{barcode}") { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode").orEmpty()
            PriceListScreen(
                navController = navController,
                viewModel     = productViewModel,
                barcode       = barcode
            )
        }

        // تفاصيل المنتج باستخدام معرف المنتج
        composable("product_details/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            ProductDetailsScreen(
                navController     = navController,
                viewModel         = productViewModel,
                productId         = productId
            )
        }

        // تفاصيل المنتج باستخدام الباركود
        composable(
            route = "product_details?barcode={barcode}",
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode").orEmpty()
            ProductDetailsScreen(
                navController     = navController,
                viewModel         = productViewModel,
                scannedBarcode    = barcode
            )
        }
        composable("notifications") {
            NotificationScreen(
                navController = navController,
                viewModel = productViewModel
            )
        }
        composable("account") {
            AccountScreen(
                navController = navController,
                viewModel = productViewModel
            )
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }





        // خريطة المتاجر – وضعي select أو pick حسب المعامل
        composable(
            route = "store_map?mode={mode}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "pick"
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "pick"
            StoreMapScreen(navController = navController, viewModel = productViewModel, mode = mode)
        }
    }
}
