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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun LessonsScreen(courseId: Int, onOpenLesson: (String) -> Unit, onBackToMenu: () -> Unit) {
//    val lessons = Repo.getLessonsByCourse(courseId)
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("TEČAJ $courseId") },
//                navigationIcon = {
//                    IconButton(onClick = onBackToMenu) {
//                        Icon(Icons.Filled.ArrowBack, contentDescription = "Povratak na menu")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//    Column(Modifier.fillMaxSize()) {
//        Text("Lekcije", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
//        LazyVerticalGrid(
//            columns = GridCells.Fixed(2),
//            contentPadding = PaddingValues(12.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(lessons) { l ->
//                Card(onClick = { onOpenLesson(l.id) }) {
//                    Column(
//                        Modifier.padding(12.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        AsyncImage(
//                            model = ImageRequest.Builder(LocalContext.current)
//                                .data(l.imageUrl).crossfade(true).build(),
//                            contentDescription = l.it,
//                            modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.large)
//                        )
//                        Spacer(Modifier.height(8.dp))
//                        Text(l.it, style = MaterialTheme.typography.titleMedium)
//                        Text(l.hr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
//                    }
//                }
//            }
//        }
//    }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(courseId: String, onOpenLesson: (String) -> Unit, onBackToMenu: () -> Unit) {
    val vm: LessonsVM = androidx.lifecycle.viewmodel.compose.viewModel(factory = LessonsVM.factory(courseId))

    Scaffold(topBar = {
        TopAppBar(title = { Text("TEČAJ") },
            navigationIcon = { IconButton(onClick = onBackToMenu) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Lekcije", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vm.items, key = { it.id }) { l ->
                    Card(onClick = { onOpenLesson(l.id) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // HR iznad, veći
                            Text(
                                l.content,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))

                            // Slika centrirana
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(l.imageUrl).crossfade(true).build(),
                                contentDescription = l.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.large)
                            )
                            Spacer(Modifier.height(8.dp))

                            // IT ispod, manjim fontom i primarnom bojom
                            Text(
                                l.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
    }
}
