package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// --- Model ---
data class Word(val id: String = "", val it: String = "", val hr: String = "")

// --- Repo (sluša promjene u stvarnom vremenu) ---
class DictionaryRepo(
    private val col: CollectionReference = Firebase.firestore.collection("dictionary")
) {
    fun listenAll(
        onUpdate: (List<Word>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        col.orderBy("it")
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener onError(e)
                val words = snap?.documents?.map {
                    Word(
                        id = it.id,
                        it = it.getString("it") ?: "",
                        hr = it.getString("hr") ?: ""
                    )
                }.orEmpty()
                onUpdate(words)
            }
}

// --- ViewModel (drži stanje i filtrira lokalno) ---
class DictionaryVM(
    private val repo: DictionaryRepo = DictionaryRepo()
) : ViewModel() {

    var query by mutableStateOf("")
        private set
    var words by mutableStateOf<List<Word>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    private var reg: ListenerRegistration? = null

    init {
        reg = repo.listenAll(
            onUpdate = {
                words = it
                loading = false
            },
            onError = {
                error = it.localizedMessage
                loading = false
            }
        )
    }

    fun onQueryChange(q: String) { query = q }

    val results: List<Word>
        get() {
            val q = query.trim()
            if (q.isBlank()) return words
            return words.filter { w ->
                w.it.contains(q, true) || w.hr.contains(q, true)
            }
        }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}

// --- UI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    vm: DictionaryVM = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rječnik") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).fillMaxSize()  ) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = vm::onQueryChange,
                label = { Text("Pretraži… (IT/HR)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (vm.error != null) Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),             // 2) lista zauzme ostatak i skrola
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.results, key = { it.id }) { w ->
                    ListItem(
                        headlineContent = { Text(w.it) },
                        supportingContent = { Text(w.hr) }
                    )
                    Divider()
                }
            }
        }
    }
}
