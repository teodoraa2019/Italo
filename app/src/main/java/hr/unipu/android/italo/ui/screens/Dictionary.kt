package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val base = listOf(
        "ciao" to "bok/pozdrav",
        "grazie" to "hvala",
        "per favore" to "molim",
        "acqua" to "voda",
        "pane" to "kruh"
    )
    val results = remember(query) {
        if (query.isBlank()) base else base.filter {
            it.first.contains(query, ignoreCase = true) || it.second.contains(query, true)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Rječnik") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Pretraži… (IT/HR)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { (itWord, hrWord) ->
                    ListItem(
                        headlineContent = { Text(itWord) },
                        supportingContent = { Text(hrWord) }
                    )
                    Divider()
                }
            }
        }
    }
}
