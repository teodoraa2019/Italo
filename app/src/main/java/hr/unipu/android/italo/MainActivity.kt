package hr.unipu.android.italo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import hr.unipu.android.italo.ui.navigation.ItaloNavGraph
import hr.unipu.android.italo.ui.theme.ItaloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItaloTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ItaloNavGraph()
                }
            }
        }
    }
}
