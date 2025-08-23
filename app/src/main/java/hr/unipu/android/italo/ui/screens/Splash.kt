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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import hr.unipu.android.italo.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // kratko zadržavanje
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4BB2F9)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Italo logo",
                modifier = Modifier.size(96.dp)
            )

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