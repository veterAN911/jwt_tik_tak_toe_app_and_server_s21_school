package com.example.tiktak_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.tiktak_app.presentation.AppNavHost
import com.example.tiktak_app.presentation.theme.TiktakTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiktakTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
