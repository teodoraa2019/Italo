package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) {
                Tab(selected = true, onClick = {}, text = { Text("TEČAJ") })
            }

            when {
                vm.error != null -> Text(
                    "Greška: ${vm.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )

                vm.groups.isEmpty() && !vm.loading -> Text(
                    "Nema pronađenih lekcija.",
                    modifier = Modifier.padding(16.dp)
                )

                else -> LazyColumn {
                    items(vm.groups, key = { it.id }) { g ->
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                            headlineContent = { Text(g.label) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onOpenGroup(g.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

data class LessonGroup(val id: String, val label: String, val count: Int)

class LessonGroupsVM(private val courseId: String) : ViewModel() {

    var groups by mutableStateOf(listOf<LessonGroup>())
        private set

    var loading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadGroups()
    }

    private fun loadGroups() = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            val candidates = buildList {
                add("lessons")
                for (i in 1..9) add("lessons_$i")
            }

            val found = mutableListOf<LessonGroup>()
            for (name in candidates) {
                val snap = db.collection("courses")
                    .document(courseId)
                    .collection(name)
                    .limit(1)
                    .get()
                    .await()

                if (!snap.isEmpty) {
                    val full = db.collection("courses")
                        .document(courseId)
                        .collection(name)
                        .get()
                        .await()

                    found += LessonGroup(
                        id = name,
                        label = toPrettyLabel(name, full.size()),
                        count = full.size()
                    )
                }
            }

            groups = found.ifEmpty { emptyList() }
        } catch (e: Exception) {
            error = e.message ?: "Nešto je pošlo po zlu."
        } finally {
            loading = false
        }
    }

    private fun toPrettyLabel(raw: String, count: Int): String {
        val base = if (raw == "lessons") "Lekcije" else "Lekcije " + raw.substringAfter('_')
        return "$base ($count)"
    }

    companion object {
        fun factory(courseId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return LessonGroupsVM(courseId) as T
                }
            }
    }
}
