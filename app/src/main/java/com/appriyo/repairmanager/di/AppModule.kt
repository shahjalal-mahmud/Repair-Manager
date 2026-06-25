// app/src/main/java/com/appriyo/repairmanager/di/AppModule.kt
package com.appriyo.repairmanager.di

import com.appriyo.repairmanager.data.repository.AppSettingsRepository
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.EmployeeNotesRepository
import com.appriyo.repairmanager.data.repository.NotesRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.data.repository.SmsLogRepository
import com.appriyo.repairmanager.data.sms.DeviceIdProvider
import com.appriyo.repairmanager.data.sms.SmsAutoSendManager
import com.appriyo.repairmanager.data.sms.SmsSender
import com.appriyo.repairmanager.presentation.viewmodel.AddRepairViewModel
import com.appriyo.repairmanager.presentation.viewmodel.AuthViewModel
import com.appriyo.repairmanager.presentation.viewmodel.CustomerDetailsViewModel
import com.appriyo.repairmanager.presentation.viewmodel.CustomerListViewModel
import com.appriyo.repairmanager.presentation.viewmodel.EditRepairViewModel
import com.appriyo.repairmanager.presentation.viewmodel.EmployeeNotesViewModel
import com.appriyo.repairmanager.presentation.viewmodel.MainViewModel
import com.appriyo.repairmanager.presentation.viewmodel.NotesViewModel
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import com.appriyo.repairmanager.presentation.viewmodel.SmsSettingsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseFirestore.getInstance() }

    // Repositories
    single { AuthRepository(get()) }
    single { RepairRepository(get()) }
    single { NotesRepository(get()) }
    single { EmployeeNotesRepository(get()) }
    single { AppSettingsRepository(get()) }
    single { SmsLogRepository(get()) }

    // SMS infrastructure
    single { DeviceIdProvider(androidContext()) }
    single { SmsSender(androidContext()) }
    single { SmsAutoSendManager(androidContext(), get(), get(), get(), get(), get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { AddRepairViewModel(get(), get(), get()) }
    viewModel { PrintViewModel(get()) }
    viewModel { CustomerListViewModel(get()) }
    viewModel { CustomerDetailsViewModel(get()) }
    viewModel { EditRepairViewModel(get()) }
    viewModel { NotesViewModel(get(), get()) }
    viewModel { EmployeeNotesViewModel(get(), get()) }
    viewModel { SmsSettingsViewModel(get(), get(), get()) }
}