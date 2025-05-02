package com.example.applicationapp.screens

import android.Manifest
import android.util.Size
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.applicationapp.components.BottomNavigationBar
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    fromShopping: Boolean = false
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val lifecycleOwner = context as LifecycleOwner
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val scannedCode = remember { mutableStateOf("") }

    // طلب إذن الكاميرا
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    // عند قراءة باركود
    LaunchedEffect(scannedCode.value) {
        if (scannedCode.value.isNotBlank()) {
            val products = viewModel.productList.value.filter {
                it.barcode == scannedCode.value
            }
            if (products.isNotEmpty()) {
                if (fromShopping) {
                    viewModel.addSelectedProduct(products.first())
                    navController.popBackStack()
                } else {
                    navController.navigate("price_list/${scannedCode.value}")
                }
            } else {
                if (fromShopping) {
                    navController.popBackStack()
                } else {
                    navController.navigate("add_product?barcode=${scannedCode.value}&storeName=")
                }
            }
        }
    }

    PricesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Barcode", color = PricesTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PricesTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
            bottomBar = {
                BottomNavigationBar(navController)
            },
            containerColor = PricesBackgroundColor
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // الكاميرا
                AndroidView(factory = { previewView })

                // Overlay مستطيل شفاف
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // مربع السكان
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clipToBounds()
                        .background(Color.Transparent)
                        .border(2.dp, PricesSelectedIconColor, RoundedCornerShape(8.dp))
                )

                // نص التعليمات
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Align the barcode within the frame",
                        color = PricesTextPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                    )
                }
            }
        }
    }

    // إعداد الكاميرا
    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy: ImageProxy ->
            processImageProxy(imageProxy) { barcodeValue ->
                if (scannedCode.value.isBlank()) {
                    scannedCode.value = barcodeValue
                }
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
    }
}

// معالجة صورة الكاميرا
private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val firstBarcode = barcodes.firstOrNull { it.rawValue != null }
                firstBarcode?.rawValue?.let { value ->
                    onBarcodeDetected(value)
                }
            }
            .addOnFailureListener {
                // خطأ بالمسح
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
