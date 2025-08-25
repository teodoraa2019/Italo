package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.unipu.android.italo.data.Repo
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onOpenCourse: (String) -> Unit,
    onOpenQuiz: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
    vm: CoursesVM = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("TEČAJEVI", "PROVJERE ZNANJA")
    var expanded by remember { mutableStateOf(false) }
    val user = Firebase.auth.currentUser
    val photoUrl = user?.photoUrl?.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MENU") },
                navigationIcon = { IconButton(onClick = { /* drawer */ }) { Icon(Icons.Filled.Menu, null) } },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        if (photoUrl.isNullOrEmpty()) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profil")
                        } else {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profilna slika",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profil") },
                            onClick = {
                                expanded = false
                                onOpenProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Odjava") },
                            onClick = {
                                expanded = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) }) }
            }

            if (selectedTab == 0) {
                when {
                    vm.error != null -> Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    vm.items.isEmpty() -> Text("Nema tečajeva.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(vm.items, key = { it.id }) { c ->
                            ListItem(
                                headlineContent = { Text(c.description) },
                                // supportingContent = { Text(c.description) },
                                leadingContent = { Icon(Icons.Filled.Star, null) },
                                modifier = Modifier.clickable { onOpenCourse(c.id) }
                            )
                            Divider()
                        }
                    }
                }
            } else {
                val quizzes = Repo.getQuizzes()
                LazyColumn {
                    items(quizzes, key = { it.id }) { q ->
                        ListItem(
                            headlineContent = { Text(q.title) },
                            leadingContent = { Icon(Icons.Filled.Star, null) },
                            modifier = Modifier.clickable { onOpenQuiz(q.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
