package com.example.applicationapp.screens

import User
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.applicationapp.R
import com.example.applicationapp.ui.theme.*
import com.example.applicationapp.viewmodel.ProductViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: ProductViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val oneTapClient: SignInClient = Identity.getSignInClient(context)
    val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId("859871452183-omta6i0gk2r1lkb0sba83qog5fe2q3gb.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()
        )
        .build()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        try {
            val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            val userName = credential.displayName ?: ""
            val userEmail = credential.id

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener {
                        val firebaseUser = auth.currentUser
                        val user = User(
                            id = firebaseUser?.uid ?: "",
                            name = userName,
                            email = userEmail,
                            imageUrl = firebaseUser?.photoUrl?.toString() ?: ""
                        )
                        viewModel.setUser(user)
                        firestore.collection("users").document(user.id).set(user)
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit().putBoolean("is_logged_in", true).apply()
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "فشل تسجيل الدخول بـ Google", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل التحقق من Google", Toast.LENGTH_SHORT).show()
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isRegisterMode) "Sign Up" else "Sign In", color = PricesTextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PricesBackgroundColor)
                )
            },
            containerColor = PricesBackgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        oneTapClient.beginSignIn(signInRequest)
                            .addOnSuccessListener { result ->
                                launcher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Google Sign-In غير متاح", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue with Google", color = Color.Black)
                }

                Text("أو", color = PricesTextPrimary)

                if (isRegisterMode) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = isRegisterMode && it != confirmPassword
                    },
                    label = { Text("كلمة المرور") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = password != it
                        },
                        isError = passwordError,
                        label = { Text("تأكيد كلمة المرور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(passwordError) {
                        Text("كلمتا المرور غير متطابقتين", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        if (isRegisterMode) {
                            if (name.isBlank() || email.isBlank() || password.isBlank() || password != confirmPassword) {
                                isLoading = false
                                Toast.makeText(context, "يرجى ملء جميع الحقول بشكل صحيح", Toast.LENGTH_SHORT).show()
                            } else {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        val firebaseUser = auth.currentUser
                                        val user = User(
                                            id = firebaseUser?.uid ?: "",
                                            name = name,
                                            email = email,
                                            imageUrl = ""
                                        )
                                        viewModel.setUser(user)
                                        firestore.collection("users").document(user.id).set(user)
                                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                            .edit().putBoolean("is_logged_in", true).apply()
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "فشل إنشاء الحساب: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    val firebaseUser = auth.currentUser
                                    val user = User(
                                        id = firebaseUser?.uid ?: "",
                                        name = "", // لاحقًا ممكن تجيبه من Firestore
                                        email = email,
                                        imageUrl = ""
                                    )
                                    viewModel.setUser(user)
                                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                        .edit().putBoolean("is_logged_in", true).apply()
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "فشل تسجيل الدخول: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PricesSelectedIconColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (isRegisterMode) "إنشاء حساب" else "دخول", color = Color.White)
                    }
                }

                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    name = ""
                    password = ""
                    confirmPassword = ""
                    passwordError = false
                }) {
                    Text(
                        if (isRegisterMode) "لديك حساب؟ تسجيل الدخول" else "ليس لديك حساب؟ إنشاء حساب",
                        color = PricesSelectedIconColor
                    )
                }
            }
        }
    }
}

