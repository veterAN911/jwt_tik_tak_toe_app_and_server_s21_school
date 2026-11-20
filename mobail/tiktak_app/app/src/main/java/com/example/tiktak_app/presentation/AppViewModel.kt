package com.example.tiktak_app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktak_app.App

@Composable
fun rememberAppViewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as App).appComponent.viewModelFactory()
    }
}

@Composable
inline fun <reified T : ViewModel> appViewModel(): T {
    val factory = rememberAppViewModelFactory()
    return viewModel<T>(factory = factory)
}
