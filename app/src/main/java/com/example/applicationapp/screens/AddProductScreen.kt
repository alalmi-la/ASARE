package com.example.applicationapp.screens

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
import com.example.applicationapp.viewmodel.ProductViewModel
import com.example.asare_montagrt.data.model.Product
import kotlinx.coroutines.launch

@Composable
fun AddProductScreen(navController: NavController, viewModel: ProductViewModel, productId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(150.dp).padding(8.dp)
            )
        }

        Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("اختر صورة")
        }

        Button(
            onClick = {
                isSaving = true
                val newProduct = Product(
                    id = productId ?: "",
                    name = name,
                    price = price.toDoubleOrNull() ?: 0.0,
                    store = store,
                    imageUrl = imageUri?.toString() ?: ""
                )
                scope.launch {
                    viewModel.addProduct(newProduct)
                    Toast.makeText(context, "تمت إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                    isSaving = false
                    navController.popBackStack()
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(if (isSaving) "جارٍ الحفظ..." else "حفظ المنتج")
        }
    }
}
