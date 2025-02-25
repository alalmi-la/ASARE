@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)
package com.example.applicationapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.applicationapp.repository.ProductRepository
import com.example.asare_montagrt.data.model.Product
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(navController: NavController, repository: ProductRepository) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var inputStoreName by remember { mutableStateOf("") }
    var inputStoreLocation by remember { mutableStateOf("") }
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var barcodeResult by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            hasCameraPermission = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مسح الباركود", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)) {
            if (hasCameraPermission) {
                CameraPreviewWithBarcodeScanner(
                    lifecycleOwner = lifecycleOwner,
                    executor = executor
                ) { scannedValue ->
                    Log.d("BarcodeScanner", "Barcode scanned: $scannedValue")
                    barcodeResult = scannedValue
                    scannedBarcode = scannedValue
                    // عرض Dialog لإدخال بيانات المتجر
                    showStoreDialog = true
                }
            } else {
                Text(text = "يرجى السماح باستخدام الكاميرا لمسح الباركود.")
            }

            barcodeResult?.let { code ->
                Text(text = "الباركود: $code", style = MaterialTheme.typography.bodyLarge)
            }

            scannedProduct?.let { product ->
                Text(text = "المنتج: ${product.name}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "السعر: ${product.price} ريال", style = MaterialTheme.typography.bodyLarge)
                Text(text = "المتجر: ${product.storeName}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "الموقع: ${product.storeLocation}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { navController.navigate("productDetails/${product.id}") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("عرض تفاصيل المنتج", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            title = { Text("أدخل بيانات المتجر") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputStoreName,
                        onValueChange = { inputStoreName = it },
                        label = { Text("اسم المتجر") }
                    )
                    OutlinedTextField(
                        value = inputStoreLocation,
                        onValueChange = { inputStoreLocation = it },
                        label = { Text("موقع المتجر") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showStoreDialog = false
                    scope.launch {
                        val product = repository.getProductByBarcodeAndStore(
                            barcode = scannedBarcode!!,
                            store = inputStoreName,
                            storeLocation = inputStoreLocation
                        )
                        if (product != null) {
                            scannedProduct = product
                        } else {
                            // إذا لم يكن المنتج موجوداً، يتم التوجه إلى شاشة إضافة المنتج مع تمرير البيانات
                            val route = "addProduct?barcode=${scannedBarcode}&storeName=${inputStoreName}&storeLocation=${inputStoreLocation}"
                            navController.navigate(route)
                        }
                    }
                },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("حفظ", color = MaterialTheme.colorScheme.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showStoreDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithBarcodeScanner(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: ExecutorService,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx)
            cameraProviderFuture.addListener(
                {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            processBarcodeImage(imageProxy, barcodeScanner, onBarcodeScanned)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Error setting up camera", e)
                    }
                },
                ContextCompat.getMainExecutor(ctx)
            )
            previewView
        }
    )
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun processBarcodeImage(
    imageProxy: ImageProxy,
    barcodeScanner: BarcodeScanner,
    onBarcodeScanned: (String) -> Unit
) {
    try {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Log.e("BarcodeError", "mediaImage is null. Skipping frame.")
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { scannedValue ->
                        Log.d("BarcodeScanner", "Scanned: $scannedValue")
                        onBarcodeScanned(scannedValue)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BarcodeError", "Error processing barcode", exception)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } catch (e: Exception) {
        Log.e("BarcodeError", "Exception in processBarcodeImage", e)
        imageProxy.close()
    }
}
