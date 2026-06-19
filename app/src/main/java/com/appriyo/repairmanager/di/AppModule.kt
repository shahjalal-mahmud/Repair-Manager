// app/src/main/java/com/appriyo/repairmanager/di/AppModule.kt
package com.appriyo.repairmanager.di

import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.presentation.viewmodel.AuthViewModel
import com.appriyo.repairmanager.presentation.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AuthRepository(get()) }

    viewModel { MainViewModel(get()) }
    viewModel { AuthViewModel(get()) }
}