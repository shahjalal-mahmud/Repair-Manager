// app/src/main/java/com/appriyo/repairmanager/MainActivity.kt
package com.appriyo.repairmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appriyo.repairmanager.presentation.screens.MainScreen
import com.appriyo.repairmanager.presentation.viewmodel.MainViewModel
import com.appriyo.repairmanager.ui.theme.RepairManagerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            val loading = viewModel.isLoading.value
            loading
        }

        enableEdgeToEdge()
        setContent {
            RepairManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startRoute by viewModel.startDestination.collectAsState()
                    MainScreen(startDestination = startRoute)
                }
            }
        }
    }
}