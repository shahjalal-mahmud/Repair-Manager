// app/src/main/java/com/appriyo/repairmanager/RepairManagerApp.kt
package com.appriyo.repairmanager

import android.app.Application
import com.appriyo.repairmanager.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class RepairManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@RepairManagerApp)
            modules(appModule)
        }
    }
}