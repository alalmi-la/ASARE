package com.example.applicationapp.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("الرئيسية", "home", Icons.Rounded.Home),
        BottomNavItem("الماسح", "barcode", Icons.Rounded.QrCodeScanner),
        BottomNavItem("التسوق", "shopping", Icons.Rounded.ShoppingCart),
        BottomNavItem("إضافة متجر", "add_store", Icons.Rounded.Add)
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                label = {
                    if (selected) {
                        Text(
                            text = item.title,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                alwaysShowLabel = false
            )
        }
    }
}
