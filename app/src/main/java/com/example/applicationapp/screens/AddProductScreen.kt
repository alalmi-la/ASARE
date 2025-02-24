package com.example.applicationapp.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    productId: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // اختيار الصورة من الجهاز
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "إضافة منتج جديد", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم المنتج") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("السعر") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = store,
            onValueChange = { store = it },
            label = { Text("المتجر") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        // عرض الصورة المختارة محليًا قبل الرفع
        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(150.dp).padding(8.dp)
            )
        }

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("اختر صورة")
        }

        Button(
            onClick = {
                isSaving = true
                if (imageUri != null) {
                    // تحويل الـ URI إلى ملف فعلي
                    val file = getFileFromUri(context, imageUri!!)
                    if (file != null) {
                        MediaManager.get().upload(file.absolutePath)
                            .option("resource_type", "image")
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String?) {
                                    // بدء عملية الرفع
                                }
                                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                                    // متابعة تقدم الرفع (اختياري)
                                }
                                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                    val secureUrl = resultData?.get("secure_url") as? String ?: ""
                                    println("Upload success, secure_url: $secureUrl")
                                    val newProduct = Product(
                                        id = productId ?: "",
                                        name = name,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        store = store,
                                        barcode = "",
                                        imageUrl = secureUrl
                                    )
                                    scope.launch {
                                        viewModel.addProduct(newProduct)
                                        Toast.makeText(context, "تمت إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                        navController.popBackStack()
                                    }
                                }
                                override fun onError(requestId: String?, error: ErrorInfo?) {
                                    Toast.makeText(context, "فشل رفع الصورة: ${error?.description}", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                }
                                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                                    // التعامل مع إعادة جدولة الرفع إن لزم الأمر
                                }
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
                        store = store,
                        barcode = "",
                        imageUrl = ""
                    )
                    scope.launch {
                        viewModel.addProduct(newProduct)
                        Toast.makeText(context, "تمت إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                        isSaving = false
                        navController.popBackStack()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(if (isSaving) "جارٍ الحفظ..." else "حفظ المنتج")
        }
    }
}
