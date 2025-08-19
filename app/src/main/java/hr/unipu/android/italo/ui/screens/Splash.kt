package hr.unipu.android.italo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // kratko zadržavanje
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8ADCF2)), // svijetloplava kao na maketi
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ovdje bi išla tvoja ikona/otisak prsta; za sad tekst:
            Text("🔆", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Italo", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun SplashScreen(onContinue: (loggedIn: Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        delay(800)
        onContinue(Firebase.auth.currentUser != null)
    }
}