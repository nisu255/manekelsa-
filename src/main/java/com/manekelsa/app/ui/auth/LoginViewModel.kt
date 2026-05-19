package com.manekelsa.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val loginState: StateFlow<Resource<FirebaseUser>?> = _loginState.asStateFlow()

    fun signInAnonymously() {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            _loginState.value = authRepository.signInAnonymously()
        }
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
