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


@Composable
fun LessonDetailScreen(
    lessonId: String,
    onNext: (String) -> Unit,
    onBackToList: () -> Unit
) {
    val lesson = Repo.getLesson(lessonId)
    if (lesson == null) {
        AssistiveError("Lekcija nije pronađena"); return
    }
    val next = Repo.getNextInCourse(lessonId)

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Lekcija", style = MaterialTheme.typography.titleMedium)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(lesson.imageUrl).build(),
            contentDescription = lesson.it,
            modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.extraLarge)
        )
        Text(lesson.it, style = MaterialTheme.typography.headlineMedium)
        Text(lesson.hr, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBackToList, modifier = Modifier.weight(1f)) { Text("Natrag") }
            Button(
                onClick = { next?.let { onNext(it.id) } },
                modifier = Modifier.weight(1f),
                enabled = next != null
            ) { Text(if (next != null) "Sljedeća" else "Kraj tečaja") }
        }
    }
}
