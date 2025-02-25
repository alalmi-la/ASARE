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
import com.example.applicationapp.ui.theme.AppTheme
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                val repository = ProductRepository(FirebaseFirestore.getInstance())

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // يتم تمرير innerPadding إلى NavigationGraph عبر تعديل الـ modifier
                    NavigationGraph(
                        navController = navController,
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}