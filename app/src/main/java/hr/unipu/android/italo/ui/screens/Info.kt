package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit, onFaq: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { p ->
        val scroll = rememberScrollState()
        Column(
            Modifier.padding(p).verticalScroll(scroll).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Italo – mobilna aplikacija za učenje talijanskog jezika uz kratke, interaktivne lekcije i prilagodbu tvom napretku.",
                style = MaterialTheme.typography.bodyLarge
            )

            Text("Što nudi:", style = MaterialTheme.typography.titleMedium)
            Text("• Interaktivne lekcije i kvizovi za provjeru znanja")
            Text("• Rječnik s brzim pretraživanjem (IT ↔ HR)")
            Text("• Više razina težine i personaliziran tempo")
            Text("• Profil s osnovnim podacima i praćenjem napretka")

            Text("Kako započeti:", style = MaterialTheme.typography.titleMedium)
            Text("1) Registriraj se ili se prijavi.")
            Text("2) Odaberi razinu i otvori lekcije.")
            Text("3) Rješavaj kvizove i testove te prati napredak u profilu.")
            Text("Savjet: Uči kratko, ali redovito – aplikacija pamti gdje si stao/la.")


            Text("Kontakt: info@italo.com", style = MaterialTheme.typography.titleMedium)

            Text(
                "Za dodatna pitanja i savjete pogledaj često postavljana pitanja.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onFaq, modifier = Modifier.fillMaxWidth()) {
                Text("Često postavljana pitanja")
            }
        }
    }
}
