package com.innovatex.auracast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.innovatex.auracast.ui.screens.AccessibilityScreen
import com.innovatex.auracast.ui.screens.RouteScreen
import com.innovatex.auracast.ui.screens.SetupCheckScreen
import com.innovatex.auracast.ui.screens.RouteConfirmScreen
import com.innovatex.auracast.ui.theme.AuracastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuracastTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RouteConfirmScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
