package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import hr.unipu.android.italo.data.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onOpenCourse: (Int) -> Unit,
    onOpenQuiz: (Int) -> Unit,
    onOpenProfile: () -> Unit,   // NOVO
    onLogout: () -> Unit         // NOVO
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("TEČAJEVI", "PROVJERE ZNANJA")
    var expanded by remember { mutableStateOf(false) } // za dropdown

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MENU") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO drawer */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profil")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Profil") },
                            onClick = { expanded = false; onOpenProfile() }
                        )
                        DropdownMenuItem(
                            text = { Text("Odjava") },
                            onClick = { expanded = false; onLogout() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                }
            }
            if (selectedTab == 0) {
                val courses = Repo.getCourses()
                LazyColumn {
                    items(courses, key = { it.id }) { c ->
                        ListItem(
                            headlineContent = { Text("${c.titleHr} - ${c.titleIt}") },
                            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                            modifier = Modifier.clickable { onOpenCourse(c.id) }
                        )
                        Divider()
                    }
                }
            } else {
                val quizzes = Repo.getQuizzes()
                LazyColumn {
                    items(quizzes, key = { it.id }) { q ->
                        ListItem(
                            headlineContent = { Text(q.title) },
                            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                            modifier = Modifier.clickable { onOpenQuiz(q.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
