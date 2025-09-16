package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onOpenLessons: (String, String) -> Unit,
    onOpenQuizzes: (String) -> Unit,
    onOpenExams: (String) -> Unit,
    onOpenUsers: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val photoUrl = Firebase.auth.currentUser?.photoUrl?.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Filled.Description, null)
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        if (photoUrl.isNullOrEmpty()) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profil")
                        } else {
                            coil.compose.AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profil",
                                modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Korisnici") }, onClick = { menuOpen = false; onOpenUsers() })
                        DropdownMenuItem(text = { Text("Profil") },    onClick = { menuOpen = false; onOpenProfile() })
                        DropdownMenuItem(text = { Text("Odjava") },    onClick = { menuOpen = false; onLogout() })
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) { Tab(true, {}, text = { Text("DOKUMENTI") }) }
            LazyColumn {
                item {
                    ListItem(headlineContent = { Text("Lekcije") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier = Modifier.clickable { onOpenLessons("a1", "a2") })
                    Divider()
                }
                item {
                    ListItem(headlineContent = { Text("Kvizovi") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier = Modifier.clickable { onOpenQuizzes("ALL") })
                    Divider()
                }
                item {
                    ListItem(headlineContent = { Text("Provjere znanja") },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier = Modifier.clickable { onOpenExams("ALL") })
                    Divider()
                }
            }
        }
    }
}
