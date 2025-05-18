package com.example.applicationapp.screens.Menu


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.applicationapp.ui.theme.AppTheme
import com.example.applicationapp.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("الحساب", style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (user != null) {

                    Image(
                        painter = rememberAsyncImagePainter(
                            model = user!!.imageUrl.ifBlank { "https://i.imgur.com/BoN9kdC.png" } // صورة افتراضية
                        ),
                        contentDescription = "صورة المستخدم",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )


                    Text(user!!.name, style = MaterialTheme.typography.titleMedium)
                    Text(user!!.email, style = MaterialTheme.typography.bodyMedium)


                    Button(
                        onClick = {
                            viewModel.logout {
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج")
                    }
                } else {
                    Text("لم يتم تسجيل الدخول", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
