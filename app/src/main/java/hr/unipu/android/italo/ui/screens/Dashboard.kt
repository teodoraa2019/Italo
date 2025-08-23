package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDictionary: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenLogin: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    Scaffold(topBar = { TopAppBar(title = { Text("Italo") }) }) { p ->
        Column(
            Modifier.padding(p).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) { Text("Prijava") }
            Button(onClick = onOpenInfo, modifier = Modifier.fillMaxWidth()) { Text("Pregled općih informacija") }
            Button(onClick = onOpenDictionary, modifier = Modifier.fillMaxWidth()) { Text("Rječnik") }
        }
    }
}
