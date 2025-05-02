package com.example.applicationapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.applicationapp.ui.theme.PricesSelectedIconColor
import com.example.applicationapp.ui.theme.PricesTextSecondary
import com.example.asare_montagrt.data.model.Product



// 🔥 منتج بارز - بطاقة عمودية
@Composable
fun ProductCardFeatured(
    product: Product,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() }
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${product.price} DZD", color = PricesSelectedIconColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${product.storeName}", style = MaterialTheme.typography.bodySmall, color = PricesTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("🔥 مميز", color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}

// 🕵️‍♂️ آخر ما بحثت عنه - ListItem أنيق
@Composable
fun ProductCardRecentSearch(
    product: Product,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = {
            Text(product.name, maxLines = 1)
        },
        supportingContent = {
            Text("${product.price} DZD", color = PricesSelectedIconColor)
        },
        leadingContent = {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        trailingContent = {
            Icon(Icons.Outlined.AccessTime, contentDescription = "Recent", tint = PricesTextSecondary)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ProductCardTopRated(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isNew = System.currentTimeMillis() - product.updatedAt < 7 * 24 * 60 * 60 * 1000 // 7 أيام

    Card(
        modifier = modifier
            .width(180.dp)
            .height(270.dp)
            .padding(8.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                if (isNew) {
                    Text(
                        text = "🆕 جديد",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(bottomEnd = 12.dp))
                    )
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            // ⭐ التقييم بالنجوم
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = if (index < product.rating.toInt()) Color(0xFFFFC107) else Color(0xFFCCCCCC),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 💰 السعر
            Text(
                text = "${product.price} DZD",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PricesSelectedIconColor
            )

            // 🏪 اسم المتجر مختصر لو طويل
            val storeDisplay = if (product.storeName.length > 20) {
                product.storeName.take(18) + "..."
            } else product.storeName

            Text(
                text = "📍 $storeDisplay",
                style = MaterialTheme.typography.bodySmall,
                color = PricesTextSecondary,
                maxLines = 1
            )
        }
    }
}

