package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onOpenLessons: (String) -> Unit,
    onOpenQuizzes: (String) -> Unit,
    onOpenExams: (String) -> Unit,
    onBackToMenu: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) {
                Tab(selected = true, onClick = {}, text = { Text("DOKUMENTI") })
            }

            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text("Lekcije") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onOpenLessons("ALL") }
                    )
                    Divider()
                }
                item {
                    ListItem(
                        headlineContent = { Text("Kvizovi") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onOpenQuizzes("ALL") }
                    )
                    Divider()
                }
                item {
                    ListItem(
                        headlineContent = { Text("Provjere znanja") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onOpenExams("ALL") }
                    )
                    Divider()
                }
            }
        }
    }
}
