package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val auth = Firebase.auth

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Registracija", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            EmailField(email, { email = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PasswordField(
                value = pass,
                onValueChange = { pass = it },
                modifier = Modifier.fillMaxWidth()
            )

            PasswordField(
                value = pass2,
                onValueChange = { pass2 = it },
                label = "Ponovi lozinku",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    error = validateRegister(email, pass, pass2)
                    if (error == null) {
                        loading = true
                        auth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener { t ->
                                loading = false
                                if (t.isSuccessful) onRegisterSuccess()
                                else error = t.exception.fbMsg()
                            }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) { Text(if (loading) "Registriram..." else "Registriraj se") }

            TextButton(onClick = onGoToLogin) { Text("Imaš račun? Prijavi se") }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                AssistiveError(error!!)
            }
        }
    }
}

private fun validateRegister(email: String, p1: String, p2: String): String? {
    if (!email.contains("@")) return "Neispravna e-mail adresa."
    if (p1.length < 6) return "Lozinka mora imati barem 6 znakova."
    if (p1 != p2) return "Lozinke se ne podudaraju."
    return null
}

