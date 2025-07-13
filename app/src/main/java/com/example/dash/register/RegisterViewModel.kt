package com.example.dash.register

import androidx.lifecycle.ViewModel

class RegisterViewModel : ViewModel() {

    fun register(name: String, email: String, password: String) {
        // Aquí pondrías lógica real: validaciones, guardar en base de datos, API, etc.
        println("Registrando usuario: $name con email: $email")
    }
}