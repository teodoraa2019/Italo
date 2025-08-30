package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonGroupsScreen(
    courseId: String,
    onOpenGroup: (String) -> Unit,
    onBack: () -> Unit,
    vm: LessonGroupsVM = viewModel(factory = LessonGroupsVM.factory(courseId))
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
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) { Tab(selected = true, onClick = {}, text = { Text("TEČAJ") }) }

            when {
                vm.error != null ->
                    Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                vm.groups.isEmpty() && !vm.loading ->
                    Text("Nema pronađenih lekcija.", modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(vm.groups, key = { it.id }) { g ->
                        val dim = g.completed
                        ListItem(
                            leadingContent = { FilledStar(percent = g.percent, size = 24.dp) },
                            headlineContent = {
                                if (g.percent > 0) {
                                    Column {
                                        Text(g.label)
                                        Text(
                                            "${g.percent}%",
                                            color = when {
                                                g.percent == 100 -> Color(0xFF4CAF50)   // zeleno
                                                g.percent >= 67 -> Color(0xFFA2FF31)   // žuto-zeleno
                                                g.percent >= 34 -> Color(0xFFFFEB3B)   // žuto
                                                g.percent > 0   -> Color(0xFFFF7043)   // narančasto
                                                else            -> Color(0xFFF61700)   // crveno
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    Text(g.label)
                                }
                            }

                            ,
                            trailingContent = {
                                if (g.completed)
                                    OutlinedButton(onClick = { vm.restartGroup(g.id) }) { Text("Restart") }
                                else
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            },
                            modifier = Modifier
                                .alpha(if (dim) 0.5f else 1f)
                                .then(if (!g.completed) Modifier.clickable { onOpenGroup(g.id) } else Modifier)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

data class LessonGroup(
    val id: String,
    val label: String,
    val count: Int,
    val percent: Int = 0,
    val completed: Boolean = false
)

class LessonGroupsVM(private val courseId: String) : ViewModel() {

    var groups by mutableStateOf(listOf<LessonGroup>()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    init { loadGroups() }
    fun reload() = loadGroups()

    fun restartGroup(groupId: String) = viewModelScope.launch {
        try {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            val db = Firebase.firestore
            val progress = db.collection("users").document(uid)
                .collection("progress").document(courseId)

            val toDelete = progress.collection("lessons")
                .whereEqualTo("groupId", groupId).get().await()
            for (d in toDelete.documents) d.reference.delete()

            progress.update("groups", FieldValue.arrayRemove(groupId)).await()

            loadGroups()
        } catch (e: Exception) { error = e.message }
    }

    private fun loadGroups() = viewModelScope.launch {
        loading = true
        try {
            val db = Firebase.firestore
            val uid = Firebase.auth.currentUser?.uid
            val candidates = buildList { add("lessons"); for (i in 1..9) add("lessons_$i") }

            val completedSet: Set<String> = if (uid != null) {
                val progress = db.collection("users").document(uid)
                    .collection("progress").document(courseId).get().await()
                (progress.get("groups") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
            } else emptySet()

            val found = mutableListOf<LessonGroup>()
            for (name in candidates) {
                val probe = db.collection("courses").document(courseId)
                    .collection(name).limit(1).get().await()
                if (!probe.isEmpty) {
                    val full = db.collection("courses").document(courseId)
                        .collection(name).get().await()
                    val total = full.size()

                    val solved = if (uid != null) {
                        db.collection("users").document(uid)
                            .collection("progress").document(courseId)
                            .collection("lessons")
                            .whereEqualTo("correct", true)
                            .whereEqualTo("groupId", name)
                            .get().await().size()
                    } else 0

                    val pct = if (total > 0) (solved * 100) / total else 0
                    found += LessonGroup(
                        id = name,
                        label = toPrettyLabel(name, total),
                        count = total,
                        percent = pct,
                        completed = name in completedSet
                    )
                }
            }
            groups = found
        } catch (e: Exception) { error = e.message } finally { loading = false }
    }

    private fun toPrettyLabel(raw: String, count: Int): String {
        val base = if (raw == "lessons") "Lekcije" else "Lekcije " + raw.substringAfter('_')
        return "$base ($count)"
    }

    companion object {
        fun factory(courseId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LessonGroupsVM(courseId) as T
                }
            }
    }
}
