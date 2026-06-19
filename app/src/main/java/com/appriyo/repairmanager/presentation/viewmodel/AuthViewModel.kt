package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _user = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    val user = _user.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.user.collect { firebaseUser ->
                _user.value = firebaseUser
            }
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient = authRepository.googleSignInClient

    fun handleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            _authState.update { AuthState.Loading }
            try {
                if (account != null) {
                    val result = authRepository.signInWithGoogle(account.idToken!!)
                    result.fold(
                        onSuccess = { firebaseUser ->
                            _authState.update { AuthState.Success(firebaseUser) }
                        },
                        onFailure = { exception ->
                            _authState.update { AuthState.Error(exception.message ?: "Sign in failed") }
                        }
                    )
                } else {
                    _authState.update { AuthState.Error("Google sign in failed") }
                }
            } catch (e: Exception) {
                _authState.update { AuthState.Error(e.message ?: "Sign in failed") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun resetState() {
        _authState.update { AuthState.Idle }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}