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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ExamTest(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = ""
)

class ExamsVM(private val examId: String, private val groupId: String) : ViewModel() {
    var items by mutableStateOf(listOf<ExamTest>()); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    private fun load() = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            val col = db.collection("exams_a1").document(examId).collection(groupId)
            val snap = col.orderBy("order").get().await()
            items = snap.documents.map {
                ExamTest(
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
        fun factory(examId: String, groupId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExamsVM(examId, groupId) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamsScreen(
    examId: String,
    groupId: String,
    onOpenTestEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: ExamsVM = viewModel(factory = ExamsVM.factory(examId, groupId))
    var editingId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("DOKUMENTI") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            Text("Testovi (uređivanje)", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vm.items, key = { it.id }) { t ->
                    Card(onClick = { editingId = t.id }) {
                        Box(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    t.title, style = MaterialTheme.typography.titleLarge,
                                    fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(t.imageUrl).crossfade(true).build(),
                                    contentDescription = t.title, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.large)
                                )
                            }
                            IconButton(onClick = { editingId = t.id }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Uredi")
                            }
                        }
                    }
                }
            }

            editingId?.let { tid ->
                EditableExamTestDialog(
                    examId = examId, groupId = groupId, testId = tid,
                    onDismiss = { editingId = null }, onSaved = { }
                )
            }

            vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
    }
}
