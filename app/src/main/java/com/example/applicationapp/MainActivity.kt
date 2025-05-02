package com.example.applicationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.example.applicationapp.navigation.NavigationGraph
import com.example.applicationapp.navigation.StartDestinationGraph
import com.example.applicationapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colorScheme.background) {
                    StartDestinationGraph(navController = navController)
                }
            }
        }
    }
}

