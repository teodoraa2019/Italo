package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import androidx.compose.ui.graphics.luminance

private val QuizPalette = listOf(
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
    Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF66BB6A),
    Color(0xFFFFCA28), Color(0xFFFF8A65), Color(0xFF78909C)
)
private fun colorForQuiz(quizId: String): Color {
    val i = abs(quizId.hashCode()) % QuizPalette.size
    return QuizPalette[i]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizGroupsScreen(
    quizId: String,
    onOpenGroupAdmin: (quizId: String, groupId: String) -> Unit,
    onBack: () -> Unit,
    vm: AdminQuizGroupsVM = viewModel(factory = AdminQuizGroupsVM.factory(quizId))
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.reload() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DOKUMENTI") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) {
                Tab(selected = true, onClick = {}, text = { Text(if (quizId.equals("ALL", true)) "SVE GRUPE" else "DOKUMENTI") })
            }

            when {
                vm.error != null ->
                    Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                vm.groups.isEmpty() && !vm.loading ->
                    Text("Nema pronađenih kviz grupa.", modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(vm.groups, key = { "${it.quizId}:${it.id}" }) { g ->
                        val accent = colorForQuiz(g.quizId)
                        val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

                        Card(
                            onClick = { onOpenGroupAdmin(g.quizId, g.id) },
                            colors = CardDefaults.cardColors(containerColor = accent),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text("${g.quizLabel} • ${g.label}") },
                                trailingContent = {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onAccent)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    headlineColor = onAccent,
                                    supportingColor = onAccent,
                                    overlineColor = onAccent,
                                    trailingIconColor = onAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AdminQuizGroup(
    val quizId: String,
    val quizLabel: String,
    val id: String,
    val label: String,
    val count: Int,
    val orderInQuiz: Int? = null
)

class AdminQuizGroupsVM(private val quizId: String) : ViewModel() {
    var groups by mutableStateOf(listOf<AdminQuizGroup>()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }
    fun reload() = load()

    private fun groupIndex(name: String): Int =
        name.substringAfter("quizzes_group_", "").toIntOrNull() ?: Int.MAX_VALUE

    private fun load() = viewModelScope.launch {
        loading = true
        try {
            val db = Firebase.firestore
            val quizzesToCheck = if (quizId.equals("ALL", true))
                db.collection("quizzes_a1").get().await().documents.map { it.id }
            else listOf(quizId)

            val out = mutableListOf<AdminQuizGroup>()
            for (qid in quizzesToCheck) {
                val meta = db.collection("quizzes_a1").document(qid).get().await()
                val quizLabel = (meta.getString("description")
                    ?: meta.getString("title") ?: qid)
                val quizOrder = meta.getLong("order")?.toInt()

                for (i in 1..50) {
                    val gname = "quizzes_group_$i"
                    val probe = db.collection("quizzes_a1").document(qid).collection(gname).limit(1).get().await()
                    if (probe.isEmpty) continue
                    val total = db.collection("quizzes_a1").document(qid).collection(gname).get().await().size()
                    out += AdminQuizGroup(
                        quizId = qid,
                        quizLabel = quizLabel,
                        id = gname,
                        label = "Grupa $i ($total)",
                        count = total,
                        orderInQuiz = quizOrder
                    )
                }
            }

            groups = out.sortedWith(
                compareBy<AdminQuizGroup>({ it.orderInQuiz ?: Int.MAX_VALUE })
                    .thenBy { it.quizLabel.lowercase() }
                    .thenBy { groupIndex(it.id) }
            )
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    companion object {
        fun factory(quizId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminQuizGroupsVM(quizId) as T
                }
            }
    }
}
