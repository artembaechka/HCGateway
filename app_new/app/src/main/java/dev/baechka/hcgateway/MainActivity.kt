package dev.baechka.hcgateway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.baechka.hcgateway.ui.navigation.AppNavigation
import dev.baechka.hcgateway.ui.theme.HCGatewayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HCGatewayTheme {
                AppNavigation()
            }
        }
    }
}