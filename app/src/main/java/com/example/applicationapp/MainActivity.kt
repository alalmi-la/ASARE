package com.example.applicationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.applicationapp.repository.ProductRepository
import com.example.applicationapp.screens.NavigationGraph
import com.example.applicationapp.ui.theme.ApplicationAPPTheme
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApplicationAPPTheme {
                val navController = rememberNavController()
                val repository = ProductRepository(FirebaseFirestore.getInstance()) // ✅ تمرير repository

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationGraph(
                        navController = navController,
                        repository = repository // ✅ تمرير repository إلى NavigationGraph
                    )
                }
            }
        }
    }
}
