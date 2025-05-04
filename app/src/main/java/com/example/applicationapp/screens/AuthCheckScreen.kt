package com.example.applicationapp.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun AuthCheckScreen(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        delay(500) // لإظهار لودينغ خفيف
        if (currentUser != null) {
            navController.navigate("home") {
                popUpTo("auth_check") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("auth_check") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
