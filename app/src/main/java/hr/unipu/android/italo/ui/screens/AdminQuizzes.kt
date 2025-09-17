package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


data class QuizTask(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = ""
)

class QuizzesVM(
    private val quizId: String,
    private val groupId: String
) : ViewModel() {
    var items by mutableStateOf(listOf<QuizTask>()); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    private fun load() = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            val col = db.collection("quizzes_a1").document(quizId).collection(groupId)
            val snap = col.orderBy("order").get().await()

            items = snap.documents.map {
                QuizTask(
                    id       = it.id,
                    title    = it.getString("question") ?: it.id,
                    imageUrl = it.getString("imageUrl") ?: ""
                )
            }
            error = null
        } catch (e: Exception) {
            error = e.message
            items = emptyList()
        }
    }

    companion object {
        fun factory(quizId: String, groupId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuizzesVM(quizId, groupId) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizzesScreen(
    quizId: String,
    groupId: String,
    onOpenTaskEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: QuizzesVM = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = QuizzesVM.factory(quizId, groupId)
    )

    var editingId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("KVIZOVI") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Zadaci (uređivanje)", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            var addingNew by remember { mutableStateOf(false) }

            Button(
                onClick = { addingNew = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Dodaj novi zadatak")
            }
            Spacer(Modifier.height(8.dp))

            if (addingNew) {
                EditableTaskDialog(
                    quizId = quizId,
                    groupId = groupId,
                    taskId = null,   // novi zadatak
                    onDismiss = { addingNew = false },
                    onSaved = { addingNew = false }
                )
            }


            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vm.items, key = { it.id }) { t ->
                    Card(onClick = { editingId = t.id }) {
                        Box(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(t.imageUrl).crossfade(true).build(),
                                    contentDescription = t.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.large)
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            IconButton(
                                onClick = { editingId = t.id },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Uredi")
                            }
                        }
                    }
                }
            }


            editingId?.let { tid ->
                EditableTaskDialog(
                    quizId = quizId,
                    groupId = groupId,
                    taskId = tid,
                    onDismiss = { editingId = null },
                    onSaved = { }
                )
            }

            vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
    }
}

suspend fun generateNextTaskIdAndOrder(
    quizId: String,
    groupId: String
): Pair<String, Int> {
    val db = Firebase.firestore
    val tasksRef = db.collection("quizzes_a1")
        .document(quizId)
        .collection(groupId)

    val snapshot = tasksRef.get().await()

    val maxOrder = snapshot.documents
        .mapNotNull { it.getLong("order")?.toInt() }
        .maxOrNull() ?: 0

    val maxIdNum = snapshot.documents.mapNotNull {
        it.id.removePrefix("task_").toIntOrNull()
    }.maxOrNull() ?: 0

    return Pair("task_${maxIdNum + 1}", maxOrder + 1)
}
