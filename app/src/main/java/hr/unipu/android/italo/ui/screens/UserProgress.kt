package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Stat(val correct: Int, val total: Int) {
    val pct: Int get() = if (total > 0) (correct * 100) / total else 0
    val frac: Float get() = if (total > 0) correct.toFloat() / total else 0f
}

data class RowPct(val label: String, val pct: Int)

class ProgressVM : ViewModel() {
    var lessons by mutableStateOf(Stat(0, 0)); private set
    var quizzes by mutableStateOf(Stat(0, 0)); private set
    var exams   by mutableStateOf(Stat(0, 0)); private set

    var byQuiz  by mutableStateOf(listOf<RowPct>()); private set
    var byExam  by mutableStateOf(listOf<RowPct>()); private set

    var loading by mutableStateOf(true); private set
    var error   by mutableStateOf<String?>(null); private set

    init { load() }

    private fun labelize(id: String, prefix: String, titleFallback: (Int) -> String): String {
        val n = id.substringAfter("${prefix.lowercase()}_", "").toIntOrNull() ?: return id
        return titleFallback(n)
    }

    fun load() = viewModelScope.launch {
        loading = true; error = null
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) { loading = false; return@launch }

        try {
            val db = Firebase.firestore

            runCatching {
                val roots = db.collection("users").document(uid).collection("progress").get().await()
                var lC = 0; var lT = 0
                var qC = 0; var qT = 0
                var eC = 0; var eT = 0

                for (course in roots.documents) {
                    // Lessons
                    val lessonsDocs = course.reference.collection("lessons").get().await().documents
                    lT += lessonsDocs.size
                    lC += lessonsDocs.count { it.getBoolean("correct") == true }

                    // Quizzes
                    val quizzesDocs = course.reference.collection("quizzes").get().await().documents
                    qT += quizzesDocs.size
                    qC += quizzesDocs.count { it.getBoolean("correct") == true }

                    // Exams
                    val examsDocs = course.reference.collection("exams").get().await().documents
                    eT += examsDocs.size
                    eC += examsDocs.count { it.getBoolean("correct") == true }
                }

                lessons = Stat(lC, lT)
                quizzes = Stat(qC, qT)
                exams   = Stat(eC, eT)

            }

            runCatching {
                val roots = db.collection("users").document(uid).collection("progress").get().await().documents
                val perQuiz = mutableMapOf<String, Pair<Int, Int>>()
                val perExam = mutableMapOf<String, Pair<Int, Int>>()
                for (course in roots) {
                    course.reference.collection("quizzes").get().await().documents.forEach { d ->
                        val id = d.getString("quizId") ?: return@forEach
                        val ok = d.getBoolean("correct") == true
                        val cur = perQuiz[id] ?: (0 to 0)
                        perQuiz[id] = (cur.first + if (ok) 1 else 0) to (cur.second + 1)
                    }
                    course.reference.collection("exams").get().await().documents.forEach { d ->
                        val id = d.getString("examId") ?: return@forEach
                        val ok = d.getBoolean("correct") == true
                        val cur = perExam[id] ?: (0 to 0)
                        perExam[id] = (cur.first + if (ok) 1 else 0) to (cur.second + 1)
                    }
                }
                byQuiz = perQuiz.entries.sortedBy { it.key }.map {
                    val (c, t) = it.value
                    RowPct(labelize(it.key, "Quiz") { n -> "Kviz $n" }, if (t > 0) (c * 100) / t else 0)
                }
                byExam = perExam.entries.sortedBy { it.key }.map {
                    val (c, t) = it.value
                    RowPct(labelize(it.key, "Exam") { n -> "Ispit $n" }, if (t > 0) (c * 100) / t else 0)
                }
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val vm = remember { ProgressVM() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Napredak") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        if (vm.loading) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(p)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error) }

            StatCard("Lekcije", vm.lessons, color = Color(0xFFF44336))
            StatCard("Kvizovi", vm.quizzes, color = Color(0xFF2196F3))
            StatCard("Ispiti", vm.exams, color = Color(0xFF4CAF50))

            if (vm.byQuiz.isNotEmpty()) {
                Text("Točnost po kvizu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                BarChart(rows = vm.byQuiz, color = Color(0xFF2196F3))
            }
            if (vm.byExam.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Točnost po ispitu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                BarChart(rows = vm.byExam, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, s: Stat, color: Color) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = s.frac,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = color.copy(alpha = 0.18f)
            )
            Text("${s.correct} / ${s.total} • ${s.pct}%")
        }
    }
}
@Composable
private fun BarChart(rows: List<RowPct>, color: Color) {
    val data = rows.take(8)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.label, style = MaterialTheme.typography.bodyMedium)
                Box(
                    Modifier.fillMaxWidth().height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(row.pct / 100f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color)
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(1.dp))
                        Text("${row.pct}%", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}