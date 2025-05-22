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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.applicationapp.components.TopBarWithLogo
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
    val user = viewModel.currentUser.collectAsState().value

    // State for fields
    var barcode by rememberSaveable { mutableStateOf(prefillBarcode) }
    var name by rememberSaveable { mutableStateOf(prefillName) }
    var price by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf(initialStoreName) }
    var imageUrl by rememberSaveable { mutableStateOf(prefillImageUrl) }
    var storeLocation by remember { mutableStateOf(initialLocation?.let {
        GeoPoint(it.first.toDouble(), it.second.toDouble())
    }) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var localUpdateMode by rememberSaveable { mutableStateOf(isUpdateMode) }

    // 1) Get the back stack entry
    val backEntry = remember("add_product") {
        navController.getBackStackEntry("add_product")
    }




    // 2) Handle edit request
    val editId by backEntry
        .savedStateHandle
        .getLiveData<String>("edit_product_id")
        .observeAsState(initial = null)
    LaunchedEffect(editId) {
        editId?.let { id ->
            viewModel.getProductByIdNow(id)?.let { p ->
                name = p.name
                price = p.price.toString()
                storeName = p.storeName
                storeLocation = p.storeLocation
                barcode = p.barcode
                imageUrl = p.imageUrl
            }
            localUpdateMode = true
            backEntry.savedStateHandle.remove<String>("edit_product_id")
        }
    }

    // 3) Observe operation result
    val opResult by viewModel.operationResult.collectAsState()
    LaunchedEffect(opResult) {
        opResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // 4) Listen for scanned barcode from StateFlow
    val scannedCode by viewModel.scannedBarcode.collectAsState()

    LaunchedEffect(Unit) {
        snapshotFlow { scannedCode }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { code ->
                barcode = code
                viewModel.clearScannedBarcode()
            }
    }



    // 5) Listen for selected store
    val selLat by backEntry
        .savedStateHandle
        .getLiveData<Double>("selected_store_lat")
        .observeAsState(initial = null)
    val selLng by backEntry
        .savedStateHandle
        .getLiveData<Double>("selected_store_lng")
        .observeAsState(initial = null)
    val selName by backEntry
        .savedStateHandle
        .getLiveData<String>("selected_store_name")
        .observeAsState(initial = null)
    LaunchedEffect(selLat, selLng, selName) {
        if (selLat != null && selLng != null && selName != null) {
            storeName = selName!!
            storeLocation = GeoPoint(selLat!!, selLng!!)
            backEntry.savedStateHandle.remove<Double>("selected_store_lat")
            backEntry.savedStateHandle.remove<Double>("selected_store_lng")
            backEntry.savedStateHandle.remove<String>("selected_store_name")
        }
    }

    // Image pick / capture launchers
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        bmp?.let {
            getFileFromBitmap(context, it)?.let { file ->
                imageUri = Uri.fromFile(file)
            }
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopBarWithLogo(
                title = if (localUpdateMode) "تعديل المنتج" else "إضافة منتج",
                showBack = true,
                onBackClick = { navController.popBackStack() }
            )
        }

    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image section ...
            Box(
                Modifier
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
                        Text("اضغط لالتقاط صورة")
                    }
                }
            }

            // Name & Price
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المنتج") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !localUpdateMode || prefillName.isEmpty()
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Store picker
            OutlinedTextField(
                value = storeName,
                onValueChange = {},
                label = { Text("اسم المتجر") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("store_map?mode=pick") }) {
                        Icon(Icons.Default.Map, contentDescription = "اختيار متجر")
                    }
                }

            )
            storeLocation?.let {
                Text("📍 ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}")
            }

            // Barcode field
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("الباركود") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !localUpdateMode || prefillBarcode.isEmpty(),
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("barcode_scanner?source=ADD_PRODUCT") }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح الباركود")
                    }

                }
            )

            // Save button
            Button(
                onClick = {
                    if (name.isBlank() || price.isBlank() || storeName.isBlank() || barcode.isBlank() || storeLocation == null) {
                        Toast.makeText(context, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    uploading = true
                    val finalPrice = price.toDoubleOrNull() ?: 0.0
                    val geo = storeLocation!!
                    fun performSave(imageLink: String) {
                        viewModel.addProductFromUI(
                            name, finalPrice, storeName, barcode,
                            imageLink, geo, user?.id ?: ""
                        )
                    }
                    if (imageUri != null) {
                        getFileFromUri(context, imageUri!!)?.let { file ->
                            MediaManager.get().upload(file.absolutePath)
                                .option("resource_type", "image")
                                .callback(object : UploadCallback {
                                    override fun onStart(req: String?) {}
                                    override fun onProgress(req: String?, bytes: Long, total: Long) {}
                                    override fun onSuccess(req: String?, res: Map<*, *>) {
                                        performSave(res["secure_url"] as? String ?: "")
                                    }
                                    override fun onError(req: String?, err: ErrorInfo?) {
                                        uploading = false
                                        Toast.makeText(context, "خطأ في الرفع", Toast.LENGTH_SHORT).show()
                                    }
                                    override fun onReschedule(r: String?, e: ErrorInfo?) {}
                                }).dispatch()
                        }
                    } else performSave(imageUrl)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uploading
            ) {
                Text(if (uploading) "جارٍ الحفظ..." else if (localUpdateMode) "تحديث المنتج" else "حفظ المنتج")
            }
        }
    }
}

// Helper functions

fun getFileFromUri(context: Context, uri: Uri): File? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        File.createTempFile("temp", ".jpg", context.cacheDir).apply {
            FileOutputStream(this).use { input.copyTo(it) }
        }
    }
} catch (_: Exception) { null }

fun getFileFromBitmap(context: Context, bitmap: Bitmap): File? = try {
    File.createTempFile("temp", ".jpg", context.cacheDir).apply {
        FileOutputStream(this).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }
} catch (_: Exception) { null }
