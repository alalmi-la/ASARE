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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.io.FileOutputStream

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

    // State for fields
    var barcode by rememberSaveable { mutableStateOf(prefillBarcode) }
    var name by rememberSaveable { mutableStateOf(prefillName) }
    var price by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf(initialStoreName) }
    var imageUrl by rememberSaveable { mutableStateOf(prefillImageUrl) }

    var storeLocation by remember {
        mutableStateOf(initialLocation?.let {
            GeoPoint(it.first.toDouble(), it.second.toDouble())
        })
    }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

    // Observe operation result to show toast & pop back
    val opResult by viewModel.operationResult.collectAsState()
    LaunchedEffect(opResult) {
        opResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // If in edit mode, preload product data
    LaunchedEffect(isUpdateMode, productId) {
        if (isUpdateMode && productId.isNotEmpty()) {
            viewModel.getProductByIdNow(productId)?.let { product ->
                name = product.name
                price = product.price.toString()
                storeName = product.storeName
                storeLocation = product.storeLocation
                barcode = product.barcode
                imageUrl = product.imageUrl
            }
        }
    }

    // SavedStateHandle for returning data
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val scannedBarcode = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val code = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("scanned_barcode")
        if (!code.isNullOrBlank()) {
            scannedBarcode.value = code
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_barcode")
        }
    }

    val selLat by savedStateHandle
        ?.getLiveData<Double>("selected_store_lat")
        ?.observeAsState()
        ?: remember { mutableStateOf<Double?>(null) }

    val selLng by savedStateHandle
        ?.getLiveData<Double>("selected_store_lng")
        ?.observeAsState()
        ?: remember { mutableStateOf<Double?>(null) }

    val selName by savedStateHandle
        ?.getLiveData<String>("selected_store_name")
        ?.observeAsState()
        ?: remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedBarcode.value, selLat, selLng, selName) {
        scannedBarcode.value?.let {
            if (barcode != it) {
                barcode = it
                // إعادة تعيين بعد الاستخدام لتجنب التكرار أو التهنيج
                scannedBarcode.value = null
            }
        }



        // use temporary vals for smart cast
        val lat = selLat
        val lng = selLng
        val mapName = selName
        if (lat != null && lng != null && mapName != null) {
            storeName = mapName
            storeLocation = GeoPoint(lat, lng)
            savedStateHandle?.remove<Double>("selected_store_lat")
            savedStateHandle?.remove<Double>("selected_store_lng")
            savedStateHandle?.remove<String>("selected_store_name")
        }
    }

    // Image pick/capture launchers
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let {
            getFileFromBitmap(context, it)?.let { file ->
                imageUri = Uri.fromFile(file)
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

        ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image section
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray, RoundedCornerShape(16.dp))
                    .clickable { cameraLauncher.launch(null) }
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
                        Icon(Icons.Default.CloudUpload, contentDescription = "رفع صورة", modifier = Modifier.size(48.dp))
                        Text("اضغط لالتقاط صورة", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Name & Price fields
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Store picker via map only
            OutlinedTextField(
                value = storeName,
                onValueChange = { /* read-only */ },
                label = { Text("اسم المتجر") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("store_map?mode=pick") }) {
                        Icon(Icons.Default.Map, contentDescription = "اختيار المتجر")
                    }
                }
            )
            storeLocation?.let {
                Text(
                    text = "📍 الموقع: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Barcode field
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("الباركود") },
                modifier = Modifier.fillMaxWidth(),
                enabled = prefillBarcode.isEmpty() || !isUpdateMode,
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("barcode_scanner?source=ADD_PRODUCT") }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح الباركود")
                    }
                }
            )

            // Save/Update button
            Button(
                onClick = {
                    if (name.isBlank() || price.isBlank() || storeName.isBlank() || barcode.isBlank() || storeLocation == null) {
                        Toast.makeText(context, "يرجى ملء كل الحقول واختيار موقع المتجر", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    uploading = true
                    val finalPrice = price.toDoubleOrNull() ?: 0.0
                    val geo = storeLocation!!

                    // After image upload or direct, call ViewModel to add or update
                    fun performSave(imageLink: String) {
                        viewModel.addProductFromUI(
                            name = name,
                            price = finalPrice,
                            storeName = storeName,
                            barcode = barcode,
                            imageUrl = imageLink,
                            storeLocation = geo
                        )
                    }

                    if (imageUri != null) {
                        getFileFromUri(context, imageUri!!)?.let { file ->
                            MediaManager.get().upload(file.absolutePath)
                                .option("resource_type", "image")
                                .callback(object : UploadCallback {
                                    override fun onStart(requestId: String?) {}
                                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                        val secureUrl = resultData?.get("secure_url") as? String ?: ""
                                        performSave(secureUrl)
                                    }
                                    override fun onError(requestId: String?, error: ErrorInfo?) {
                                        uploading = false
                                        Toast.makeText(context, "خطأ في رفع الصورة", Toast.LENGTH_SHORT).show()
                                    }
                                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                                })
                                .dispatch()
                        }
                    } else {
                        performSave(imageUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uploading
            ) {
                Text(
                    when {
                        uploading -> "جارٍ الحفظ..."
                        isUpdateMode -> "تحديث المنتج"
                        else -> "حفظ المنتج"
                    }
                )
            }
        }
    }
}

// Helper functions

fun getFileFromUri(context: Context, uri: Uri): File? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        File.createTempFile("temp_image", ".jpg", context.cacheDir).apply {
            FileOutputStream(this).use { input.copyTo(it) }
        }
    }
} catch (_: Exception) { null }

fun getFileFromBitmap(context: Context, bitmap: Bitmap): File? = try {
    File.createTempFile("temp_image", ".jpg", context.cacheDir).apply {
        FileOutputStream(this).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }
} catch (_: Exception) { null }
