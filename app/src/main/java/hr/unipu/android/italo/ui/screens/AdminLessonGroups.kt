package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import androidx.compose.ui.graphics.luminance

private val CoursePalette = listOf(
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
    Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF66BB6A),
    Color(0xFFFFCA28), Color(0xFFFF8A65), Color(0xFF78909C)
)

private fun colorForCourse(courseId: String): Color {
    val i = abs(courseId.hashCode()) % CoursePalette.size
    return CoursePalette[i]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLessonGroupsScreen(
    level: String,
    courseId: String,
    onOpenGroupAdmin: (level: String, courseId: String, groupId: String) -> Unit,
    onBack: () -> Unit,
    vm: AdminLessonGroupsVM = viewModel(factory = AdminLessonGroupsVM.factory(level, courseId))
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
                title = { Text(if (courseId.equals("ALL", true)) "LEKCIJE" else "DOKUMENTI") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = 0) {
                Tab(selected = true, onClick = {}, text = { Text(if (courseId.equals("ALL", true)) "SVE LEKCIJE" else "DOKUMENTI") })
            }

            when {
                vm.error != null ->
                    Text("Greška: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                vm.groups.isEmpty() && !vm.loading ->
                    Text("Nema pronađenih lekcija.", modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(vm.groups, key = { "${it.courseId}:${it.id}" }) { g ->
                        val accent = colorForCourse(g.courseId)
                        val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

                        ListItem(
                            headlineContent = {
                                Text("${g.courseLabel} • ${g.label}")
                            },
                            trailingContent = {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onAccent)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = accent,
                                headlineColor  = onAccent,
                                supportingColor = onAccent,
                                overlineColor   = onAccent,
                                trailingIconColor = onAccent
                            ),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onOpenGroupAdmin(level, g.courseId, g.id) }
                        )
                    }
                }
            }
        }
    }
}

data class AdminLessonGroup(
    val level: String,
    val courseId: String,
    val courseLabel: String,
    val id: String,
    val label: String,
    val count: Int,
    val orderInCourse: Int? = null
)

class AdminLessonGroupsVM(private val levelFilter: String?, private val courseId: String) : ViewModel() {
    var groups by mutableStateOf(listOf<AdminLessonGroup>()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }
    fun reload() = load()

    private fun groupIndex(name: String): Int =
        name.substringAfter("lessons_group_", "").toIntOrNull() ?: Int.MAX_VALUE

    private fun labelForCourse(metaA1: Map<String, Any?>?, fallbackMeta: Map<String, Any?>?, cid: String): String {
        fun s(m: Map<String, Any?>?, k: String) = (m?.get(k) as? String)?.takeIf { it.isNotBlank() }
        return s(metaA1, "description") ?: s(metaA1, "title")
        ?: s(fallbackMeta, "description") ?: s(fallbackMeta, "title") ?: cid
    }

    private fun load() = viewModelScope.launch {
        loading = true
        try {
            val levels = if (levelFilter == null || levelFilter.equals("ALL", true)) {
                listOf("a1", "a2")
            } else {
                listOf(levelFilter)
            }

            val out = mutableListOf<AdminLessonGroup>()
            val db = Firebase.firestore

            for (level in levels) {
                val courses = db.collection("courses_$level").get().await()
                for (c in courses.documents) {
                    val courseId = c.id
                    val courseLabel = c.getString("description") ?: c.getString("title") ?: courseId
                    val courseOrder = c.getLong("order")?.toInt()

                    for (i in 1..20) {
                        val gname = "lessons_group_$i"
                        val probe = db.collection("courses_$level").document(courseId)
                            .collection(gname).limit(1).get().await()
                        if (!probe.isEmpty) {
                            val total = db.collection("courses_$level").document(courseId)
                                .collection(gname).get().await().size()
                            out += AdminLessonGroup(
                                level = level,
                                courseId = courseId,
                                courseLabel = "[$level] $courseLabel",
                                id = gname,
                                label = "Lekcija $i ($total)",
                                count = total,
                                orderInCourse = courseOrder
                            )
                        }
                    }
                }
            }

            groups = out.sortedWith(
                compareBy<AdminLessonGroup>({ it.orderInCourse ?: Int.MAX_VALUE })
                    .thenBy { it.courseLabel.lowercase() }
                    .thenBy { groupIndex(it.id) }
            )

        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    companion object {
        fun factory(level: String?, courseId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminLessonGroupsVM(level, courseId) as T
                }
            }
    }
}
