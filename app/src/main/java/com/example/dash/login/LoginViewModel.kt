package com.example.dash.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun validate(): Boolean {
        if (_email.value.isBlank()) {
            _errorMessage.value = "Por favor ingresa tu correo"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()) {
            _errorMessage.value = "Correo inválido"
            return false
        }
        if (_password.value.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        _errorMessage.value = null
        return true
    }
}
