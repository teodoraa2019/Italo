package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val user = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil korisnika") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { p ->
        Column(
            Modifier.padding(p).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = user?.photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp)
            )
            Text(user?.displayName ?: "Bez imena", style = MaterialTheme.typography.titleMedium)
            Text(user?.email ?: "Bez e‑pošte", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Uredi profil")
            }
        }
    }
}
