package com.example.applicationapp.screens.product

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.applicationapp.ui.theme.OnPrimaryColor
import com.example.applicationapp.ui.theme.PrimaryColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    initialBarcode: String? = null,
    initialName: String? = null,
    initialImageUrl: String? = null,
    initialStoreName: String? = null,
    productId: String? = null,
    isUpdateMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var barcode by remember { mutableStateOf(initialBarcode ?: "") }
    var name by remember { mutableStateOf(initialName ?: "") }
    var price by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf(initialStoreName ?: "") }
    var storeLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf(initialImageUrl) }

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
                title = { Text("إضافة منتج", color = OnPrimaryColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = OnPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
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
                    .clickable {
                        cameraCaptureLauncher.launch(null)
                    }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "صورة المنتج",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (imageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "صورة موجودة",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "رفع صورة", modifier = Modifier.size(48.dp))
                        Text("اضغط لالتقاط صورة", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المنتج") },
                modifier = Modifier.fillMaxWidth(),
                enabled = initialName.isNullOrBlank()
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
                    IconButton(onClick = {
                        navController.navigate("store_map?readonly=false")
                    }) {
                        Icon(Icons.Default.Map, contentDescription = "اختيار الموقع")
                    }
                }
            )

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("الباركود") },
                modifier = Modifier.fillMaxWidth(),
                enabled = initialBarcode.isNullOrBlank()
            )

            storeLocation?.let {
                Text("📍 الموقع المختار: ${it.latitude}, ${it.longitude}", style = MaterialTheme.typography.bodySmall)
            }

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
                                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                            val secureUrl = resultData?.get("secure_url") as? String ?: ""
                                            saveProduct(name, price, storeName, storeLocation, barcode, secureUrl, viewModel, context, navController)
                                            uploading = false
                                        }
                                        override fun onError(requestId: String?, error: ErrorInfo?) {
                                            uploading = false
                                            Toast.makeText(context, "خطأ في رفع الصورة", Toast.LENGTH_SHORT).show()
                                        }
                                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                                    })
                                    .dispatch()
                            }
                        } else if (imageUrl != null) {
                            saveProduct(name, price, storeName, storeLocation, barcode, imageUrl!!, viewModel, context, navController)
                            uploading = false
                        } else {
                            Toast.makeText(context, "يرجى اختيار صورة", Toast.LENGTH_SHORT).show()
                            uploading = false
                        }
                    } else {
                        Toast.makeText(context, "يرجى ملء كل الحقول", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !uploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uploading) "جارٍ الحفظ..." else "حفظ المنتج")
            }
        }
    }
}

private fun saveProduct(
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
    val newProduct = com.example.asare_montagrt.data.model.Product(
        id = UUID.randomUUID().toString(),
        name = name,
        price = price.toDoubleOrNull() ?: 0.0,
        storeName = storeName,
        storeLocation = storeLocation,
        barcode = barcode,
        imageUrl = imageUrl
    )
    viewModel.addProductFromUI(
        name = newProduct.name,
        price = newProduct.price,
        storeName = newProduct.storeName,
        barcode = newProduct.barcode,
        imageUrl = newProduct.imageUrl
    )
    Toast.makeText(context, "تمت إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show()
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
