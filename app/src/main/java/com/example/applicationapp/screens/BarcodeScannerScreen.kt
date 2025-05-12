package com.example.applicationapp.screens

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.BarcodeSource
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
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

    // ✅ اطلب صلاحية الكاميرا
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    // ✅ تنظيف الباركود عند الخروج من الشاشة لأي سبب
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearScannedBarcode()
        }
    }

    // ✅ معالجة نتيجة المسح
    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        val products = viewModel.getProductsByBarcodeNow(code)

        when (barcodeSource) {
            BarcodeSource.SMART_SHOPPING -> {
                if (products.isNotEmpty()) viewModel.addSelectedProduct(products.first())
                navController.popBackStack()
            }
            BarcodeSource.ADD_PRODUCT -> {
                val previous = navController.previousBackStackEntry
                if (previous != null) {
                    previous.savedStateHandle.remove<String>("scanned_barcode")
                    previous.savedStateHandle.set("scanned_barcode", code)
                    navController.popBackStack()
                } else {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_barcode", code)

                    navController.navigate("add_product") {
                        popUpTo("barcode_scanner") { inclusive = true }
                    }
                }
            }



            BarcodeSource.HOME, BarcodeSource.PRICE_LIST, null -> {
                if (products.isNotEmpty()) {
                    viewModel.setScannedBarcode(code)
                    navController.navigate("product_details?barcode=$code")
                } else {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_barcode", code)
                    navController.navigate("add_product")
                }
            }
        }

        // ✅ تأكد أن يتم تنظيف الباركود بعد التنقل
        viewModel.clearScannedBarcode()
    }

    // ✅ واجهة المستخدم
    PricesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Barcode Scanner", color = PricesTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PricesTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
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

    // ✅ تشغيل الكاميرا وتحليل الصورة
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted) return@LaunchedEffect

        val provider = cameraProviderFuture.get()
        val preview = androidx.camera.core.Preview.Builder().build().also {
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
