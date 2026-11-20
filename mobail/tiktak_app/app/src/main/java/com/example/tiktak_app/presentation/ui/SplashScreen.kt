package com.example.tiktak_app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiktak_app.presentation.appViewModel
import com.example.tiktak_app.presentation.viewmodel.AuthViewModel
import com.example.tiktak_app.presentation.viewmodel.AutoLoginState

@Composable
fun SplashScreen(
    onSuccess: () -> Unit,
    onNeedLogin: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = appViewModel()
) {
    val state by viewModel.autoLoginState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startAutoLogin()
    }

    LaunchedEffect(state) {
        when (val current = state) {
            AutoLoginState.Checking -> Unit
            AutoLoginState.Success -> onSuccess()
            is AutoLoginState.NeedLogin -> onNeedLogin(current.message)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Проверка сессии...", style = MaterialTheme.typography.bodyLarge)
    }
}
