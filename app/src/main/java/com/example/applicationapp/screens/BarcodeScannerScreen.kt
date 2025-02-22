@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)
package com.example.applicationapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.applicationapp.repository.ProductRepository
import com.example.asare_montagrt.data.model.Product
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(navController: NavController, repository: ProductRepository) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    var barcodeResult = remember { mutableStateOf<String?>(null) }
    var hasCameraPermission = remember { mutableStateOf(false) }
    var scannedProduct = remember { mutableStateOf<Product?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission.value = granted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            hasCameraPermission.value = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "امسح الباركود", style = MaterialTheme.typography.headlineMedium)

        if (hasCameraPermission.value) {
            CameraPreviewWithBarcodeScanner(
                lifecycleOwner = lifecycleOwner,
                executor = executor
            ) { scannedBarcode ->
                Log.d("BarcodeScanner", "Barcode scanned: $scannedBarcode")
                barcodeResult.value = scannedBarcode
                scope.launch {
                    val existingProduct = repository.getProductById(scannedBarcode)
                    if (existingProduct != null) {
                        scannedProduct.value = existingProduct
                    } else {
                        // إذا لم يكن المنتج موجودًا، يتم حفظه تلقائيًا ببيانات مبدئية
                        val newProduct = Product(
                            id = scannedBarcode,
                            name = "منتج جديد",
                            price = 0.0,
                            store = "غير معروف",
                            barcode = scannedBarcode,
                            imageUrl = ""
                        )
                        repository.addProduct(newProduct)
                        scannedProduct.value = newProduct
                    }
                }
            }
        } else {
            Text(text = "يرجى السماح باستخدام الكاميرا لمسح الباركود.")
        }

        barcodeResult.value?.let { code ->
            Text(text = "الباركود: $code", style = MaterialTheme.typography.bodyLarge)
        }

        scannedProduct.value?.let { product ->
            Text(text = "المنتج: ${product.name}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "السعر: ${product.price} ريال", style = MaterialTheme.typography.bodyLarge)
            Text(text = "المتجر: ${product.store}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { navController.navigate("productDetails/${product.id}") }) {
                Text("عرض تفاصيل المنتج")
            }
        }
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
