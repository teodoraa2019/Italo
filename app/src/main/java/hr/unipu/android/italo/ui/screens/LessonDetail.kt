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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenLesson(it.id) } }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodna")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    all.forEachIndexed { i, l ->
                        val selected = i == curIndex
                        AssistChip(
                            onClick = { onOpenLesson(l.id) },
                            label = { Text("${i + 1}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
                IconButton(enabled = next != null, onClick = { next?.let { onOpenLesson(it.id) } }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeća")
                }
            }
        }
    }
}

