package com.example.applicationapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.asare_montagrt.data.model.Product

@Composable
fun ProductItem(product: Product, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("productDetails/${product.id}") },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (product.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = product.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = "السعر: ${product.price} ريال", style = MaterialTheme.typography.bodyLarge)
                Text(text = "المتجر: ${product.store}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

