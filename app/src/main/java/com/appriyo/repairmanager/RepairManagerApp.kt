// app/src/main/java/com/appriyo/repairmanager/RepairManagerApp.kt
package com.appriyo.repairmanager

import android.app.Application
import com.appriyo.repairmanager.data.sms.SmsAutoSendManager
import com.appriyo.repairmanager.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.get

class RepairManagerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@RepairManagerApp)
            modules(appModule)
        }

        // Start SMS Auto Send Manager
        val smsAutoSendManager: SmsAutoSendManager = get(SmsAutoSendManager::class.java)
        smsAutoSendManager.start(appScope)
    }
}