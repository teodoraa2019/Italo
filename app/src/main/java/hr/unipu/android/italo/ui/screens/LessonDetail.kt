package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    groupId: String,
    courseId: String,
    lessonId: String,
    onBackToList: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val vm: LessonsVM = androidx.lifecycle.viewmodel.compose.viewModel(factory = LessonsVM.factory(groupId = groupId, courseId = courseId))
    val lesson = vm.items.firstOrNull { it.id == lessonId } ?: run {
        AssistiveError("Lekcija nije pronađena"); return
    }
    val all = vm.items
    val curIndex = all.indexOfFirst { it.id == lessonId }
    val prev = all.getOrNull(curIndex - 1)
    val next = all.getOrNull(curIndex + 1)

    Scaffold(topBar = {
        TopAppBar(title = { Text("POVRATAK NA MENU") },
            navigationIcon = { IconButton(onClick = onBackToList) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(lesson.imageUrl).build(),
                contentDescription = lesson.title,
                modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.extraLarge)
            )
            Text(lesson.content, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(lesson.title, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenLesson(it.id) } }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodna")
                }

                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val total = all.size
                    val start = maxOf(0, curIndex - 2)
                    val end = minOf(total - 1, curIndex + 1)

                    for (i in start..end) {
                        val selected = i == curIndex
                        AssistChip(
                            onClick = { onOpenLesson(all[i].id) },
                            label = {
                                Text(
                                    text = "${i + 1}",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.widthIn(min = 44.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    val isLast = curIndex == all.lastIndex

                    LaunchedEffect(courseId, groupId, isLast) {
                        if (isLast) {
                            try { markGroupCompleted(courseId, groupId) } catch (_: Exception) { }
                        }
                    }

                }

                IconButton(enabled = next != null, onClick = { next?.let { onOpenLesson(it.id) } }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeća")
                }
            }
        }
    }
}

suspend fun markGroupCompleted(courseId: String, groupId: String) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val ref = db.collection("users").document(uid).collection("progress").document(courseId)
    db.runTransaction { tx ->
        val cur = tx.get(ref)
        val groups = (cur.get("groups") as? List<*>)?.mapNotNull { it as? String }?.toMutableSet() ?: mutableSetOf()
        groups += groupId
        tx.set(ref, mapOf("groups" to groups))
    }.await()
}


