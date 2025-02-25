package com.example.applicationapp.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.applicationapp.ui.theme.AccentColor
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.Modifier

// دالة تحويل الـ URI إلى ملف مؤقت
fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// دالة تحويل Bitmap إلى ملف
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String?,
    isUpdateMode: Boolean = false,
    initialBarcode: String? = null,
    initialStoreName: String? = null,
    initialStoreLocation: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var barcode by remember { mutableStateOf(initialBarcode ?: "") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf(initialStoreName ?: "") }
    var storeLocation by remember { mutableStateOf(initialStoreLocation ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // إذا كان وضع التحديث، نملأ الحقول بالبيانات الحالية
    if (isUpdateMode && productId != null) {
        val products by viewModel.productList.collectAsState(initial = emptyList())
        val existingProduct = products.find { it.id == productId }
        existingProduct?.let { prod ->
            name = prod.name
            price = prod.price.toString()
            storeName = prod.storeName
            storeLocation = prod.storeLocation
            barcode = prod.barcode
        }
    }

    // لانشر اختيار الصورة من المعرض
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // لانشر التقاط صورة مباشرةً من الكاميرا
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val file = getFileFromBitmap(context, it)
            if (file != null) {
                imageUri = Uri.fromFile(file)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUpdateMode) "تحديث المنتج" else "إضافة منتج جديد", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize() // تأكد من استيراد androidx.compose.foundation.layout.fillMaxSize
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = { /* لا يسمح بالتعديل على الباركود */ },
                label = { Text("الباركود") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = false
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المنتج") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isUpdateMode
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isUpdateMode
            )
            OutlinedTextField(
                value = storeLocation,
                onValueChange = { storeLocation = it },
                label = { Text("موقع المتجر") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isUpdateMode
            )

            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "صورة المنتج",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp) // تأكد من استيراد androidx.compose.foundation.layout.size
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("اختر من المعرض", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { cameraCaptureLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("التقط صورة", color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // تأكد من استيراد androidx.compose.foundation.layout.height

            Button(
                onClick = {
                    isSaving = true
                    if (imageUri != null) {
                        val file = getFileFromUri(context, imageUri!!)
                        if (file != null) {
                            MediaManager.get().upload(file.absolutePath)
                                .option("resource_type", "image")
                                .callback(object : UploadCallback {
                                    override fun onStart(requestId: String?) {}
                                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                        val secureUrl = resultData?.get("secure_url") as? String ?: ""
                                        val newProduct = Product(
                                            id = productId ?: "",
                                            name = name,
                                            price = price.toDoubleOrNull() ?: 0.0,
                                            storeName = storeName,
                                            storeLocation = storeLocation,
                                            barcode = barcode,
                                            imageUrl = secureUrl
                                        )
                                        scope.launch {
                                            if (isUpdateMode) {
                                                viewModel.updateProduct(newProduct)
                                            } else {
                                                viewModel.addProduct(newProduct)
                                            }
                                            Toast.makeText(
                                                context,
                                                if (isUpdateMode) "تم تحديث المنتج بنجاح!" else "تمت إضافة المنتج بنجاح!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSaving = false
                                            navController.popBackStack()
                                        }
                                    }
                                    override fun onError(requestId: String?, error: ErrorInfo?) {
                                        Toast.makeText(context, "فشل رفع الصورة: ${error?.description}", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                    }
                                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                                })
                                .dispatch()
                        } else {
                            Toast.makeText(context, "فشل تحويل الصورة إلى ملف!", Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }
                    } else {
                        val newProduct = Product(
                            id = productId ?: "",
                            name = name,
                            price = price.toDoubleOrNull() ?: 0.0,
                            storeName = storeName,
                            storeLocation = storeLocation,
                            barcode = barcode,
                            imageUrl = ""
                        )
                        scope.launch {
                            if (isUpdateMode) {
                                viewModel.updateProduct(newProduct)
                            } else {
                                viewModel.addProduct(newProduct)
                            }
                            Toast.makeText(
                                context,
                                if (isUpdateMode) "تم تحديث المنتج بنجاح!" else "تمت إضافة المنتج بنجاح!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSaving = false
                            navController.popBackStack()
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Text(
                    if (isSaving) "جارٍ الحفظ..."
                    else if (isUpdateMode) "تحديث المنتج"
                    else "حفظ المنتج",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
