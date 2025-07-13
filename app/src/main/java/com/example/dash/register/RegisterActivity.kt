package com.example.dash.register

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.dash.register.RegisterScreen
import com.example.dash.register.RegisterViewModel
import com.example.dash.ui.theme.DashTheme

class RegisterActivity : ComponentActivity() {
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashTheme {
                RegisterScreen(viewModel = viewModel)
            }
        }
    }
}
