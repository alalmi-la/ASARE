package com.example.applicationapp.screens

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.components.TopBarWithLogo
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.BarcodeSource
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.example.asare_montagrt.data.model.Product
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    imageProxy.image?.let { mediaImage ->
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        BarcodeScanning.getClient()
            .process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.rawValue != null }
                    ?.rawValue
                    ?.let(onBarcodeDetected)
            }
            .addOnCompleteListener { imageProxy.close() }
    } ?: imageProxy.close()
}

@OptIn(
    ExperimentalGetImage::class,
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val barcodeSource by viewModel.barcodeSource.collectAsState()
    val isNavigated = remember { mutableStateOf(false) }

    // طلب إذن الكاميرا
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    // تنظيف الباركود عند الخروج
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearScannedBarcode()
        }
    }

    // التعامل مع الباركود عند المسح
    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        if (isNavigated.value) return@LaunchedEffect
        isNavigated.value = true

        val localProducts = viewModel.getProductsByBarcodeNow(code)
        if (localProducts.isNotEmpty()) {
            handleBarcodeNavigation(code, localProducts.first(), navController, viewModel)
        } else {
            viewModel.fetchProductByBarcodeFromDatabase(code) { remoteProduct ->
                handleBarcodeNavigation(code, remoteProduct, navController, viewModel)
            }
        }
    }

    // واجهة المستخدم
    PricesTheme {
        Scaffold(
            topBar = {
                TopBarWithLogo(
                    title = "Barcode Scanner",
                    showBack = true,
                    onBackClick = { navController.popBackStack() }
                )
            }
            ,
            bottomBar = { BottomNavigationBar(navController) },
            containerColor = PricesBackgroundColor
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(factory = { previewView })
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                Box(
                    Modifier
                        .size(250.dp)
                        .border(2.dp, PricesSelectedIconColor, RoundedCornerShape(8.dp))
                )
                Text(
                    text = "ضع الباركود داخل الإطار",
                    color = PricesTextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
            }
        }
    }

    // إعداد الكاميرا وتشغيل التحليل
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted) return@LaunchedEffect

        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
            processImageProxy(proxy) { barcodeValue ->
                if (viewModel.scannedBarcode.value != barcodeValue) {
                    viewModel.setScannedBarcode(barcodeValue)
                }
            }
        }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }
}

private fun handleBarcodeNavigation(
    barcode: String,
    product: Product?,
    navController: NavController,
    viewModel: ProductViewModel
) {
    when (viewModel.barcodeSource.value) {
        BarcodeSource.SMART_SHOPPING -> {
            if (product != null) {
                viewModel.addSelectedProduct(product)
            }
            navController.popBackStack()
        }

        BarcodeSource.ADD_PRODUCT -> {
            if (product == null) {
                navController.navigate("add_product")
            } else {
                viewModel.setEditingProduct(product)
                navController.navigate("add_product")
            }
        }

        BarcodeSource.HOME, BarcodeSource.PRICE_LIST, null -> {
            if (product != null) {
                navController.navigate("product_details?barcode=$barcode")
            } else {
                navController.navigate("add_product") {
                    popUpTo("add_product") { inclusive = true }
                }
            }
        }
    }
}
