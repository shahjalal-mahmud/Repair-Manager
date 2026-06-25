package com.appriyo.repairmanager.di

import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.viewmodel.AddRepairViewModel
import com.appriyo.repairmanager.presentation.viewmodel.AuthViewModel
import com.appriyo.repairmanager.presentation.viewmodel.CustomerDetailsViewModel
import com.appriyo.repairmanager.presentation.viewmodel.CustomerListViewModel
import com.appriyo.repairmanager.presentation.viewmodel.EditRepairViewModel
import com.appriyo.repairmanager.presentation.viewmodel.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseFirestore.getInstance() }

    // Repositories
    single { AuthRepository(get()) }
    single { RepairRepository(get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { AddRepairViewModel(get(), get()) }
    viewModel { CustomerListViewModel(get()) }
    viewModel { CustomerDetailsViewModel(get()) }
    viewModel { EditRepairViewModel(get()) }
}