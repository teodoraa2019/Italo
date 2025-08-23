package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onBack: () -> Unit) {
    val faqs = listOf(
        "Kako funkcioniraju lekcije?" to "Lekcije su kratke jedinice s kvizom na kraju.",
        "Trebam li internet?" to "Za većinu sadržaja da; preuzimanje izvanmrežno planirano.",
        "Kako promijeniti profilnu sliku?" to "Profil → Uredi profil → Odaberi sliku."
    )
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Često postavljana pitanja") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            faqs.forEach { (q, a) ->
                var open by remember { mutableStateOf(false) }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .clickable { open = !open }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(q, style = MaterialTheme.typography.titleMedium)
                        if (open) {
                            Spacer(Modifier.height(8.dp))
                            Text(a, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
