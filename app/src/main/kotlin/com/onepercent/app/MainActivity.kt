package com.onepercent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onepercent.app.navigation.OnePercentNavGraph
import com.onepercent.app.ui.theme.OnePercentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnePercentTheme {
                OnePercentNavGraph()
            }
        }
    }
}
