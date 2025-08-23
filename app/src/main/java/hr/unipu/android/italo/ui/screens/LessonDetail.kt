package hr.unipu.android.italo.ui.screens

import hr.unipu.android.italo.data.Repo
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
    lessonId: String,
    onNext: (String) -> Unit,
    onBackToList: () -> Unit,
    onOpenLesson: (String) -> Unit  // NOVO
) {
    val lesson = Repo.getLesson(lessonId) ?: run {
        AssistiveError("Lekcija nije pronađena"); return
    }

    // Kolekcija svih lekcija u ovom tečaju + indexi
    val all = remember(lesson.courseId) { Repo.getLessonsByCourse(lesson.courseId) }
    val curIndex = all.indexOfFirst { it.id == lessonId }
    val prev = all.getOrNull(curIndex - 1)
    val next = all.getOrNull(curIndex + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- sadržaj lekcije (kao kod tebe) ---
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(lesson.imageUrl).build(),
                contentDescription = lesson.it,
                modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.extraLarge)
            )
            Text(lesson.hr, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(lesson.it, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.weight(1f))

            // --- paginacija: ◀ 1 2 3 ... ▶ ---
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenLesson(it.id) } }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodna")
                }

                // brojevi (ograniči na 10-12 ako želiš)
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

            // Dodatno: dva gumba ako želiš ostaviti i stari bottom
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                OutlinedButton(onClick = onBackToList, modifier = Modifier.weight(1f)) { Text("Natrag") }
//                Button(
//                    onClick = { next?.let { onOpenLesson(it.id) } },
//                    modifier = Modifier.weight(1f),
//                    enabled = next != null
//                ) { Text(if (next != null) "Sljedeća" else "Kraj tečaja") }
//            }
        }
    }
}

