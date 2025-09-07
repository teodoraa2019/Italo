package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onGoToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val auth = Firebase.auth
    val db   = Firebase.firestore
    val scope = rememberCoroutineScope()

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
                    error = if (email.isBlank() || pass.isBlank()) "Unesite e-mail i lozinku." else null
                    if (error == null) {
                        loading = true
                        scope.launch {
                            try {
                                auth.signInWithEmailAndPassword(email.trim(), pass).await()
                                val uid = auth.currentUser?.uid
                                var isAdmin = false
                                if (uid != null) {
                                    val snap = db.collection("users").document(uid).get().await()
                                    val role = snap.getString("role") ?: "user"
                                    isAdmin = role == "admin"
                                }
                                onLoginSuccess(isAdmin)
                            } catch (ex: Exception) {
                                error = ex.fbMsg()
                            } finally {
                                loading = false
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRequiredScreen(
    onGoToLogin: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Potrebna prijava") }) }) { p ->
        Column(
            Modifier.padding(p).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Za pristup lekcijama potrebno je prijaviti se.")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onGoToLogin) { Text("Prijava") }
                OutlinedButton(onClick = onBack) { Text("Natrag") }
            }
        }
    }
}
