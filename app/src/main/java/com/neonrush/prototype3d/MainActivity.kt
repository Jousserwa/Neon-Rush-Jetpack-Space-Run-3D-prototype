package com.neonrush.prototype3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrototypeHome()
        }
    }
}

@Composable
private fun PrototypeHome() {
    val background = Color(0xFF070B16)
    val cyan = Color(0xFF35E7FF)

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            color = background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NEON RUSH",
                    color = cyan,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "3D PROTOTYPE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Standalone renderer test environment",
                    color = Color(0xFF9AA8BC),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Text(
                    text = "Next milestone: real-time 3D scene",
                    color = cyan,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 36.dp)
                )
            }
        }
    }
}
