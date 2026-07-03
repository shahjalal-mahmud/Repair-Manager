// app/src/main/java/com/appriyo/repairmanager/MainActivity.kt
package com.appriyo.repairmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appriyo.repairmanager.notifications.AlarmScheduler
import com.appriyo.repairmanager.notifications.NotificationNavigator
import com.appriyo.repairmanager.notifications.ReminderNotificationHelper
import com.appriyo.repairmanager.presentation.screens.MainScreen
import com.appriyo.repairmanager.presentation.viewmodel.MainViewModel
import com.appriyo.repairmanager.ui.theme.RepairManagerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* If denied, the daily reminder simply won't be shown by the OS. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            val loading = viewModel.isLoading.value
            loading
        }

        requestNotificationPermissionIfNeeded()
        ReminderNotificationHelper.createChannel(this)
        if (AlarmScheduler.canScheduleExactAlarms(this)) {
            AlarmScheduler.scheduleDailyReminder(this)
        }
        handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(ReminderNotificationHelper.EXTRA_NAVIGATE_ROUTE)
        if (!route.isNullOrBlank()) {
            NotificationNavigator.navigateTo(route)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}