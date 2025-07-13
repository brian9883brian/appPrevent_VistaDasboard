package com.example.dash.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dash.login.LoginScreen
import com.example.dash.login.LoginViewModel
import com.example.dash.MapActivity
import com.example.dash.register.RegisterActivity
import com.example.dash.ui.theme.DashTheme
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DashTheme {
                val loginViewModel: LoginViewModel = viewModel()
                val context = this@LoginActivity

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        context.startActivity(Intent(context, MapActivity::class.java))
                        finish()
                    },
                    onRegisterClick = {
                        context.startActivity(Intent(context, RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

