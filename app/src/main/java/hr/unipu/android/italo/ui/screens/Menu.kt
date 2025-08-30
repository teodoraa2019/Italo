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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import hr.unipu.android.italo.data.Repo

private fun starColor(pct: Int): Color {
    val p = pct.coerceIn(0, 100)
    return when {
        p == 0   -> Color(0xFFF61700)        // crveno
        p < 34   -> Color(0xFFFF7043)        // narančasto
        p < 67   -> Color(0xFFFFEB3B)        // žuto
        p < 100  -> Color(0xFFA2FF31)        // žuto-zeleno
        else     -> Color(0xFF4CAF50)        // zeleno
    }.copy(alpha = 0.85f.coerceAtMost(0.35f + p/100f * 0.65f))
}

@Composable
fun FilledStar(percent: Int, size: Dp = 24.dp) {
    val p = percent.coerceIn(0, 100)
    val tint = starColor(p)

    Box(Modifier.size(size)) {
        Icon(
            Icons.Filled.Star, null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.matchParentSize()
        )
        Icon(
            Icons.Filled.Star, null,
            tint = if (p == 0) MaterialTheme.colorScheme.outline else tint,
            modifier = Modifier.matchParentSize().padding(1.dp)
        )
    }
}

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

    val uid = Firebase.auth.currentUser?.uid
    var coursePercents by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    suspend fun computePercents(): Map<String, Int> {
        if (uid == null || vm.items.isEmpty()) return emptyMap()
        val db = Firebase.firestore
        val out = mutableMapOf<String, Int>()

        for (course in vm.items) {
            var totalLessons = 0
            val candidates = buildList {
                add("lessons")
                for (i in 1..9) add("lessons_$i")
            }
            for (name in candidates) {
                val probe = db.collection("courses").document(course.id).collection(name)
                    .limit(1).get().await()
                if (!probe.isEmpty) {
                    val snap = db.collection("courses").document(course.id).collection(name)
                        .get().await()
                    totalLessons += snap.size()
                }
            }

            val correctSnap = db.collection("users").document(uid)
                .collection("progress").document(course.id)
                .collection("lessons")
                .whereEqualTo("correct", true)
                .get().await()
            val correctLessons = correctSnap.size()

            val pct = if (totalLessons > 0) (correctLessons * 100) / totalLessons else 0
            out[course.id] = pct
        }
        return out
    }

    LaunchedEffect(selectedTab, refreshTick, vm.items) {
        if (selectedTab != 0) return@LaunchedEffect
        try {
            coursePercents = computePercents()
        } catch (_: Exception) {
            coursePercents = emptyMap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MENU") },
                navigationIcon = {
                    IconButton(onClick = { /* drawer */ }) {
                        Icon(Icons.Filled.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        if (photoUrl.isNullOrEmpty()) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profil")
                        } else {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profilna slika",
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
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
                when {
                    vm.error != null ->
                        Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    vm.items.isEmpty() ->
                        Text("Nema tečajeva.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(vm.items, key = { it.id }) { c ->
                            ListItem(
                                headlineContent = { Text(c.description) },
                                leadingContent = {
                                    val pct = coursePercents[c.id] ?: 0
                                    FilledStar(percent = pct, size = 24.dp)
                                },
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
                            modifier = Modifier.clickable { onOpenQuiz(q.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
