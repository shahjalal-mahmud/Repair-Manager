package com.appriyo.repairmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.navigation.NavGraph
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.ui.theme.RepairManagerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        enableEdgeToEdge()
        setContent {
            RepairManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // CRITICAL CHANGE: Use 'by' or collectAsState() so Compose knows to
                    // recompose the NavGraph when the route updates from Splash
                    val startRoute by viewModel.startDestination.collectAsState()

                    NavGraph(startDestination = startRoute)
                }
            }
        }
    }
}

// Simple ViewModel to mimic checking authentication or Koin state loading
class MainViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    var startDestination = MutableStateFlow(Screen.Login.route)
        private set

    init {
        viewModelScope.launch {
            // Simulate background checking (Koin init, network check, or shared preferences token checking)
            delay(2000)

            val userIsLoggedIn = false // Replace with actual logic
            if (userIsLoggedIn) {
                startDestination.value = Screen.Dashboard.route
            } else {
                startDestination.value = Screen.Login.route
            }

            // Finish loading, native splash screen automatically dismisses
            _isLoading.value = false
        }
    }
}