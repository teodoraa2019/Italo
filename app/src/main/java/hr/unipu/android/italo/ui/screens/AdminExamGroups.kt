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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Delete

private val ExamPalette = listOf(
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
    Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF66BB6A),
    Color(0xFFFFCA28), Color(0xFFFF8A65), Color(0xFF78909C)
)
private fun colorForExam(examId: String): Color {
    val i = abs(examId.hashCode()) % ExamPalette.size
    return ExamPalette[i]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamGroupsScreen(
    examId: String,
    onOpenGroupAdmin: (examId: String, groupId: String) -> Unit,
    onBack: () -> Unit,
    vm: AdminExamGroupsVM = viewModel(factory = AdminExamGroupsVM.factory(examId))
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (examId.equals("ALL", true)) "PROVJERE ZNANJA" else "DOKUMENTI") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }

    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = 0) {
                Tab(
                    selected = true,
                    onClick = {},
                    text = { Text(if (examId.equals("ALL", true)) "SVE PROVJERE ZNANJA" else "DOKUMENTI") })
            }

            var showAddDialog by remember { mutableStateOf(false) }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("Dodaj novi ispit")
            }

            if (showAddDialog) {
                AddExamDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, description ->
                        vm.addExam(title, description)
                        showAddDialog = false
                    }
                )
            }

            when {
                vm.error != null ->
                    Text(
                        "Greška: ${vm.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )

                vm.groups.isEmpty() && !vm.loading ->
                    Text("Nema pronađenih grupa ispita.", modifier = Modifier.padding(16.dp))

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(vm.groups, key = { "${it.examId}:${it.id}" }) { g ->
                        val accent = colorForExam(g.examId)
                        val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
                        Card(
                            onClick = { onOpenGroupAdmin(g.examId, g.id) },
                            colors = CardDefaults.cardColors(containerColor = accent),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text("${g.examLabel} • ${g.label}") },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { vm.deleteExam(g.examId) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Obriši",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = onAccent
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    headlineColor = onAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AdminExamGroup(
    val examId: String,
    val examLabel: String,
    val id: String,
    val label: String,
    val count: Int,
    val orderInExam: Int? = null
)

class AdminExamGroupsVM(private val examId: String) : ViewModel() {
    var groups by mutableStateOf(listOf<AdminExamGroup>()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }
    fun reload() = load()

    private fun groupIndex(name: String): Int =
        name.substringAfter("exams_group_", "").toIntOrNull() ?: Int.MAX_VALUE

    fun addExam(title: String, description: String) = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            val examsRef = db.collection("exams_a1")

            val snapshot = examsRef.get().await()
            val maxOrder = snapshot.documents.mapNotNull { it.getLong("order")?.toInt() }.maxOrNull() ?: 0
            val maxIdNum = snapshot.documents.mapNotNull {
                it.id.removePrefix("exam_").toIntOrNull()
            }.maxOrNull() ?: 0

            val nextOrder = maxOrder + 1
            val nextId = "exam_${maxIdNum + 1}"

            val data = mapOf(
                "title" to title,
                "description" to description,
                "order" to nextOrder
            )

            examsRef.document(nextId).set(data).await()
            reload()
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun deleteExam(examId: String) = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            db.collection("exams_a1").document(examId).delete().await()
            reload()
        } catch (e: Exception) {
            error = e.message
        }
    }

    private fun load() = viewModelScope.launch {
        loading = true
        try {
            val db = Firebase.firestore
            val examsToCheck = if (examId.equals("ALL", true))
                db.collection("exams_a1").get().await().documents.map { it.id }
            else listOf(examId)

            val out = mutableListOf<AdminExamGroup>()
            for (eid in examsToCheck) {
                val meta = db.collection("exams_a1").document(eid).get().await()
                val examLabel = (meta.getString("description") ?: meta.getString("title") ?: eid)
                val examOrder = meta.getLong("order")?.toInt()

                for (i in 1..50) {
                    val gname = "exams_group_$i"
                    val probe = db.collection("exams_a1").document(eid).collection(gname).limit(1).get().await()
                    if (probe.isEmpty) continue
                    val total = db.collection("exams_a1").document(eid).collection(gname).get().await().size()
                    out += AdminExamGroup(
                        examId = eid, examLabel = examLabel,
                        id = gname, label = "Grupa $i ($total)", count = total,
                        orderInExam = examOrder
                    )
                }
            }

            groups = out.sortedWith(
                compareBy<AdminExamGroup>({ it.orderInExam ?: Int.MAX_VALUE })
                    .thenBy { it.examLabel.lowercase() }
                    .thenBy { groupIndex(it.id) }
            )
        } catch (e: Exception) {
            error = e.message
        } finally { loading = false }
    }

    companion object {
        fun factory(examId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminExamGroupsVM(examId) as T
                }
            }
    }
}

@Composable
fun AddExamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description) },
                enabled = title.isNotBlank()
            ) { Text("Spremi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        },
        title = { Text("Novi ispit") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Naziv ispita") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis ispita") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
