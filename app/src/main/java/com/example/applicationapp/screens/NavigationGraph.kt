package com.example.applicationapp.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

        // شاشة التحقق من الدخول
        composable("auth_check") {
            AuthCheckScreen(navController = navController)
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

        // إضافة منتج (فارغ)
        // إضافة منتج (فارغ)
        composable("add_product") {
            AppTheme {
                // لن نمرر lat or lng أو productId من هنا
                AddProductScreen(
                    navController = navController,
                    viewModel     = productViewModel
                )
            }


        }

        // قائمة الأسعار حسب الباركود
        composable("price_list/{barcode}") { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode").orEmpty()
            PriceListScreen(navController = navController, viewModel = productViewModel, barcode = barcode)
        }

        // تفاصيل المنتج باستخدام معرف المنتج
        composable("product_details/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            ProductDetailsScreen(navController = navController, viewModel = productViewModel, productId = productId)
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
            ProductDetailsScreen(navController = navController, viewModel = productViewModel, scannedBarcode = barcode)
        }

        // تعديل منتج (تفاصيل مملوءة)
        composable(
            route = "add_product_screen?" +
                    "name={name}&barcode={barcode}&imageUrl={imageUrl}" +
                    "&storeName={storeName}&lat={lat}&lng={lng}" +
                    "&productId={productId}&isUpdateMode={isUpdateMode}",
            arguments = listOf(
                navArgument("name")       { type = NavType.StringType; defaultValue = "" },
                navArgument("barcode")    { type = NavType.StringType; defaultValue = "" },
                navArgument("imageUrl")   { type = NavType.StringType; defaultValue = "" },
                navArgument("storeName")  { type = NavType.StringType; defaultValue = "" },
                navArgument("lat")        { type = NavType.FloatType;  defaultValue = 0f },
                navArgument("lng")        { type = NavType.FloatType;  defaultValue = 0f },
                navArgument("productId")  { type = NavType.StringType; defaultValue = "" },
                navArgument("isUpdateMode"){ type = NavType.BoolType;   defaultValue = false }
            )
        ) { backStackEntry ->
            val name         = backStackEntry.arguments?.getString("name").orEmpty()
            val barcode      = backStackEntry.arguments?.getString("barcode").orEmpty()
            val imageUrl     = backStackEntry.arguments?.getString("imageUrl").orEmpty()
            val storeName    = backStackEntry.arguments?.getString("storeName").orEmpty()
            val lat          = backStackEntry.arguments?.getFloat("lat") ?: 0f
            val lng          = backStackEntry.arguments?.getFloat("lng") ?: 0f
            val productId    = backStackEntry.arguments?.getString("productId").orEmpty()
            val isUpdateMode = backStackEntry.arguments?.getBoolean("isUpdateMode") ?: false

            AppTheme {
                AddProductScreen(
                    navController     = navController,
                    viewModel         = productViewModel,
                    prefillName       = name,
                    prefillBarcode    = barcode,
                    prefillImageUrl   = imageUrl,
                    initialStoreName  = storeName,
                    initialLocation   = if (lat != 0f || lng != 0f) Pair(lat, lng) else null,
                    productId         = productId,
                    isUpdateMode      = isUpdateMode
                )
            }
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
