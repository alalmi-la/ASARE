package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var isEmailSent by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "إعادة تعيين كلمة المرور", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني") },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )

        Button(
            onClick = { isEmailSent = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "إرسال رابط إعادة التعيين")
        }

        if (isEmailSent) {
            Text(
                text = "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(onClick = { navController.popBackStack() }) {
            Text("عودة إلى تسجيل الدخول")
        }
    }
}
