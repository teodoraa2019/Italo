package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val auth = Firebase.auth

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Prijava", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            EmailField(email, { email = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PasswordField(value = pass, onValueChange = { pass = it }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    error = if (email.isBlank() || pass.isBlank()) "Unesi e-mail i lozinku." else null
                    if (error == null) {
                        loading = true
                        auth.signInWithEmailAndPassword(email, pass)
                            .addOnCompleteListener { t ->
                                loading = false
                                if (t.isSuccessful) onLoginSuccess()
                                else error = t.exception.fbMsg()
                            }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (loading) "Prijavljujem..." else "Prijavi se") }

            TextButton(onClick = onGoToRegister) { Text("Nemaš račun? Registriraj se") }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                AssistiveError(error!!)
            }
        }
    }
}
