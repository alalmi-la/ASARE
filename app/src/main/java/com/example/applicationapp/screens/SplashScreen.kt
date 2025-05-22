package com.example.applicationapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.applicationapp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // ⏳ الانتقال التلقائي بعد 2 ثانية
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("auth_check") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 🛍️ أيقونة التطبيق
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Icon",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🟢 اسم التطبيق
            Text(
                text = "Compare Price",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔎 وصف إنجليزي
            Text(
                text = "Scan barcodes. Compare prices. Save money.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            // 🌍 وصف عربي
            Text(
                text = "قارن الأسعار بسهولة – وفر في كل عملية شراء!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}
