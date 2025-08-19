package hr.unipu.android.italo.ui.screens

import hr.unipu.android.italo.data.Repo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.*


@Composable
fun LessonsScreen(courseId: Int, onOpenLesson: (String) -> Unit) {
    val lessons = Repo.getLessonsByCourse(courseId)
    Column(Modifier.fillMaxSize()) {
        Text("Lekcije", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lessons) { l ->
                Card(onClick = { onOpenLesson(l.id) }) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(l.imageUrl).crossfade(true).build(),
                            contentDescription = l.it,
                            modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.large)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(l.it, style = MaterialTheme.typography.titleMedium)
                        Text(l.hr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
