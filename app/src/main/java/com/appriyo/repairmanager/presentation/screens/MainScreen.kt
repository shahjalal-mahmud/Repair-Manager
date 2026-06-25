// app/src/main/java/com/appriyo/repairmanager/presentation/screens/MainScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.appriyo.repairmanager.navigation.NavGraph
import com.appriyo.repairmanager.presentation.components.BottomNavigationBar

@Composable
fun MainScreen(
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        )
    }
}