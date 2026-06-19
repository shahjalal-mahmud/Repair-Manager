package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow(Screen.Login.route)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                // Real auth check using repository state
                if (authRepository.isUserLoggedIn()) {
                    _startDestination.value = Screen.Dashboard.route
                } else {
                    _startDestination.value = Screen.Login.route
                }
            } catch (e: Exception) {
                // Fallback to login state safely if repository call fails
                _startDestination.value = Screen.Login.route
            } finally {
                _isLoading.value = false
            }
        }
    }
}