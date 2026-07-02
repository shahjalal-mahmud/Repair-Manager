// app/src/main/java/com/appriyo/repairmanager/di/AppModule.kt
package com.appriyo.repairmanager.di

import com.appriyo.repairmanager.data.media.MediaRepository
import com.appriyo.repairmanager.data.media.MediaStorageManager
import com.appriyo.repairmanager.data.media.NoteMediaStore
import com.appriyo.repairmanager.data.repository.AppSettingsRepository
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.EmployeeNotesRepository
import com.appriyo.repairmanager.data.repository.FirestoreUserProvider
import com.appriyo.repairmanager.data.repository.NotesRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.data.repository.SmsLogRepository
import com.appriyo.repairmanager.data.repository.TaliKhataRepository
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
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }

    // Resolves users/{uid} for every repository - the single source of truth
    // for per-account Firestore data isolation.
    single { FirestoreUserProvider(get(), get()) }

    // Repositories
    single { AuthRepository(get()) }
    single { RepairRepository(get(), get()) }
    single { NotesRepository(get(), get()) }
    single { EmployeeNotesRepository(get(), get()) }
    single { AppSettingsRepository(get(), get()) }
    single { SmsLogRepository(get(), get()) }
    single { TaliKhataRepository(get(), get()) }

    // Local-only media (never synced to Firestore)
    single { MediaStorageManager(androidContext()) }
    single { MediaRepository(androidContext()) }
    single { NoteMediaStore(androidContext()) }

    // SMS infrastructure
    single { DeviceIdProvider(androidContext()) }
    single { SmsSender(androidContext()) }
    single { SmsAutoSendManager(androidContext(), get(), get(), get(), get(), get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { AddRepairViewModel(get(), get(), get()) }
    viewModel { PrintViewModel(get()) }
    viewModel { CustomerListViewModel(get(), get()) }
    viewModel { CustomerDetailsViewModel(get(), get()) }
    viewModel { EditRepairViewModel(get()) }
    viewModel { NotesViewModel(get(), get(), get(), get()) }
    viewModel { EmployeeNotesViewModel(get(), get()) }
    viewModel { SmsSettingsViewModel(get(), get(), get()) }
    viewModel { TaliKhataViewModel(get(), get()) }
}