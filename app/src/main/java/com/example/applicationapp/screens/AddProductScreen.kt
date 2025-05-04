package com.example.applicationapp.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    prefillName: String = "",
    prefillBarcode: String = "",
    prefillImageUrl: String = "",
    initialStoreName: String = "",
    initialLocation: Pair<Float, Float>? = null,
    productId: String = "",
    isUpdateMode: Boolean = false
) {
    val context = LocalContext.current

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "يرجى تفعيل صلاحية الموقع أولاً", Toast.LENGTH_SHORT).show()
        }
    }

    var barcode by remember { mutableStateOf(prefillBarcode) }
    var name by remember { mutableStateOf(prefillName) }
    var price by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf(initialStoreName) }
    var storeLocation by remember {
        mutableStateOf(
            initialLocation?.let {
                GeoPoint(it.first.toDouble(), it.second.toDouble())
            }
        )
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf(prefillImageUrl) }

    LaunchedEffect(isUpdateMode, productId) {
        if (isUpdateMode && productId.isNotEmpty()) {
            viewModel.getProductById(productId)?.let { product ->
                name = product.name
                price = product.price.toString()
                storeName = product.storeName
                storeLocation = product.storeLocation
                barcode = product.barcode
                imageUrl = product.imageUrl
            }
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val scannedBarcode = savedStateHandle?.getLiveData<String>("scanned_barcode")?.observeAsState()
    val selectedStoreName = savedStateHandle?.getLiveData<String>("selected_store_name")?.observeAsState()
    val selectedStoreLocation = savedStateHandle?.getLiveData<GeoPoint>("selected_store_location")?.observeAsState()

    DisposableEffect(scannedBarcode?.value, selectedStoreName?.value, selectedStoreLocation?.value) {
        scannedBarcode?.value?.let {
            barcode = it
            savedStateHandle?.remove<String>("scanned_barcode")
        }
        selectedStoreName?.value?.let {
            storeName = it
            savedStateHandle?.remove<String>("selected_store_name")
        }
        selectedStoreLocation?.value?.let {
            storeLocation = it
            savedStateHandle?.remove<GeoPoint>("selected_store_location")
        }
        onDispose { }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    val cameraCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val tempFile = getFileFromBitmap(context, it)
            if (tempFile != null) {
                imageUri = Uri.fromFile(tempFile)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUpdateMode) "تعديل المنتج" else "إضافة منتج", color = OnPrimaryColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = OnPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Check permission first
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission granted, get location
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            storeLocation = GeoPoint(location.latitude, location.longitude)
                            Toast.makeText(
                                context,
                                "تم تحديد الموقع الحالي",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "تعذر الحصول على الموقع",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            context,
                            "خطأ أثناء الحصول على الموقع",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Request permission
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "تحديد موقعي")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray, RoundedCornerShape(16.dp))
                    .clickable { cameraCaptureLauncher.launch(null) }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageUri != null -> Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "صورة المنتج",
                        modifier = Modifier.fillMaxSize()
                    )
                    imageUrl.isNotEmpty() -> Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "صورة موجودة",
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "رفع صورة",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("اضغط لالتقاط صورة", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المنتج") },
                modifier = Modifier.fillMaxWidth(),
                enabled = prefillName.isEmpty() || !isUpdateMode
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("store_map_select") }) {
                        Icon(Icons.Default.Map, contentDescription = "اختيار المتجر")
                    }
                }
            )

            storeLocation?.let {
                Text(
                    text = "📍 الموقع: ${it.latitude}, ${it.longitude}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("الباركود") },
                modifier = Modifier.fillMaxWidth(),
                enabled = prefillBarcode.isEmpty() || !isUpdateMode,
                trailingIcon = {
                    IconButton(onClick = {
                        navController.navigate("barcode_scanner?source=ADD_PRODUCT")
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح الباركود")
                    }
                }

            )

            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank() && storeName.isNotBlank() && barcode.isNotBlank()) {
                        uploading = true
                        val uploadNewImage = imageUri != null
                        if (uploadNewImage) {
                            val file = getFileFromUri(context, imageUri!!)
                            if (file != null) {
                                MediaManager.get().upload(file.absolutePath)
                                    .option("resource_type", "image")
                                    .callback(object : UploadCallback {
                                        override fun onStart(requestId: String?) {}
                                        override fun onProgress(
                                            requestId: String?,
                                            bytes: Long,
                                            totalBytes: Long
                                        ) {
                                        }

                                        override fun onSuccess(
                                            requestId: String?,
                                            resultData: Map<*, *>?
                                        ) {
                                            val secureUrl =
                                                resultData?.get("secure_url") as? String
                                                    ?: ""
                                            saveProduct(
                                                productId,
                                                isUpdateMode,
                                                name,
                                                price,
                                                storeName,
                                                storeLocation,
                                                barcode,
                                                secureUrl,
                                                viewModel,
                                                context,
                                                navController
                                            )
                                            uploading = false
                                        }

                                        override fun onError(
                                            requestId: String?,
                                            error: ErrorInfo?
                                        ) {
                                            uploading = false
                                            Toast.makeText(
                                                context,
                                                "خطأ في رفع الصورة",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onReschedule(
                                            requestId: String?,
                                            error: ErrorInfo?
                                        ) {}
                                    })
                                    .dispatch()
                            }
                        } else if (imageUrl.isNotEmpty()) {
                            saveProduct(
                                productId,
                                isUpdateMode,
                                name,
                                price,
                                storeName,
                                storeLocation,
                                barcode,
                                imageUrl,
                                viewModel,
                                context,
                                navController
                            )
                            uploading = false
                        } else {
                            Toast.makeText(
                                context,
                                "يرجى اختيار صورة",
                                Toast.LENGTH_SHORT
                            ).show()
                            uploading = false
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "يرجى ملء كل الحقول",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = !uploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uploading) "جارٍ الحفظ..."
                    else if (isUpdateMode) "تحديث المنتج"
                    else "حفظ المنتج"
                )
            }
        }
    }
}

// بقية الدوال (saveProduct, getFileFromUri, getFileFromBitmap) تبقى كما كانت

fun saveProduct(
    productId: String?,
    isUpdateMode: Boolean,
    name: String,
    price: String,
    storeName: String,
    storeLocation: GeoPoint?,
    barcode: String,
    imageUrl: String,
    viewModel: ProductViewModel,
    context: Context,
    navController: NavController
) {
    val finalId = productId ?: UUID.randomUUID().toString()
    val product = Product(
        id = finalId,
        name = name,
        price = price.toDoubleOrNull() ?: 0.0,
        storeName = storeName,
        storeLocation = storeLocation,
        barcode = barcode,
        imageUrl = imageUrl
    )

    if (isUpdateMode) {
        viewModel.updateProduct(product)
        Toast.makeText(context, "تم تحديث المنتج بنجاح", Toast.LENGTH_SHORT).show()
    } else {
        viewModel.addProductFromUI(
            name = product.name,
            price = product.price,
            storeName = product.storeName,
            barcode = product.barcode,
            imageUrl = product.imageUrl
        )
        Toast.makeText(context, "تمت إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show()
    }

    navController.popBackStack()
}

fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileFromBitmap(context: Context, bitmap: Bitmap): File? {
    return try {
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

