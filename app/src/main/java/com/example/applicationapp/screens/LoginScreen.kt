package com.example.applicationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "تسجيل الدخول", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { navController.navigate("home") }
                        .addOnFailureListener { loginError = "حدث خطأ أثناء إنشاء الحساب!" }
                } else {
                    loginError = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "إنشاء حساب جديد")
        }
        TextButton(
            onClick = { navController.navigate("home") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("تخطي تسجيل الدخول")
        }




        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { navController.navigate("home") }
                        .addOnFailureListener { loginError = "تأكد من صحة بياناتك!" }
                } else {
                    loginError = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "تسجيل الدخول")
        }

        loginError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        TextButton(onClick = { navController.navigate("forgotPassword") }) {
            Text("نسيت كلمة المرور؟")
        }
    }
}
