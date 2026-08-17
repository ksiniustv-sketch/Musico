package com.Ksinius.musico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.Ksinius.musico.ui.screens.MainScreen
import com.Ksinius.musico.ui.theme.MusicoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicoTheme {
                MainScreen()
            }
        }
    }
}