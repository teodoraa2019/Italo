package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private data class QuizMeta(
    val id: String,
    val title: String // description ili fallback "Kviz N"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    courseId: String,
    onOpenQuiz: (String) -> Unit,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf(listOf<QuizMeta>()) }
    var percents by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true; error = null
        try {
            val db = Firebase.firestore
            val found = mutableListOf<QuizMeta>()

            // 1) Pronađi postojeće kvizove i povuci description
            for (i in 1..30) {
                val qid = "quiz_$i"
                val doc = db.collection("quizzes_a1").document(qid).get().await()
                if (doc.exists()) {
                    val title = doc.getString("description") ?: "Kviz $i"
                    found += QuizMeta(qid, title)
                }
            }
            items = found

            // 2) Izračun postotaka po kvizu (sve grupe + svi taskovi)
            val uid = Firebase.auth.currentUser?.uid
            if (uid != null && found.isNotEmpty()) {
                val out = mutableMapOf<String, Int>()
                for (q in found) {
                    var total = 0
                    for (g in 1..30) {
                        val gid = "quizzes_group_$g"
                        val probe = db.collection("quizzes_a1").document(q.id)
                            .collection(gid).limit(1).get().await()
                        if (!probe.isEmpty) {
                            total += db.collection("quizzes_a1").document(q.id)
                                .collection(gid).get().await().size()
                        }
                    }
                    val solved = db.collection("users").document(uid)
                        .collection("progress").document(courseId)
                        .collection("quizzes")
                        .whereEqualTo("quizId", q.id)
                        .whereEqualTo("correct", true)
                        .get().await().size()

                    out[q.id] = if (total > 0) (solved * 100) / total else 0
                }
                percents = out
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text("Greška: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            items.isEmpty() -> Text("Nema kvizova.", modifier = Modifier.padding(16.dp))
            else -> Column(Modifier.fillMaxSize().padding(pad)) {
                TabRow(selectedTabIndex = 0) { Tab(selected = true, onClick = {}, text = { Text("KVIZOVI") }) }

                LazyColumn {
                    items(items, key = { it.id }) { q ->
                        val pct = percents[q.id] ?: 0
                        ListItem(
                            leadingContent = { FilledStar(percent = pct, size = 24.dp) },
                            headlineContent = {
                                Column {
                                    Text(q.title) // ← prikaz descriptiona
                                    if (pct > 0) Text(
                                        "$pct%",
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onOpenQuiz(q.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
