package com.example.do_an_app_adr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.do_an_app_adr.ui.AnglApp
import com.example.do_an_app_adr.ui.theme.Do_an_app_adrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Do_an_app_adrTheme {
                AnglApp()
            }
        }
    }
}
