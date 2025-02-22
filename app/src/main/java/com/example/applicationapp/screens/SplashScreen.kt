package com.example.applicationapp.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.applicationapp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "fadeAnimation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000) // مدة العرض قبل الانتقال
        navController.navigate("login") { popUpTo("splash") { inclusive = true } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6200EE)), // لون مميز للخلفية
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon), // تأكد من وجود الشعار داخل مجلد res/drawable
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .alpha(alphaAnim.value)
        )
    }
}

