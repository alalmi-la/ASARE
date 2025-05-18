package com.example.applicationapp.screens

import User
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthCheckScreen(
    navController: NavController,
    productViewModel: ProductViewModel
) {
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val user = User(
                id = currentUser.uid,
                name = currentUser.displayName ?: "",
                email = currentUser.email ?: "",
                imageUrl = currentUser.photoUrl?.toString() ?: ""
            )

            // ✅ تحديث ViewModel بالحساب الحالي
            productViewModel.setUser(user)

            navController.navigate("home") {
                popUpTo("auth_check") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("auth_check") { inclusive = true }
            }
        }
    }
}
