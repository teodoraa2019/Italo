package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import hr.unipu.android.italo.data.LessonsVM
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLessonsScreen(
    courseId: String,
    groupId: String,
    onOpenLessonEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: LessonsVM = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LessonsVM.factory(courseId, groupId)
    )

    var editingId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("DOKUMENTI") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Lekcije (uređivanje)", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vm.items, key = { it.id }) { l ->
                    Card(onClick = { editingId = l.id }) {
                        Box(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    l.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(l.imageUrl).crossfade(true).build(),
                                    contentDescription = l.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.large)
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            IconButton(
                                onClick = { editingId = l.id },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Uredi")
                            }
                        }
                    }
                }
            }

            editingId?.let { lid ->
                EditableLessonDialog(
                    courseId = courseId,
                    groupId  = groupId,
                    lessonId = lid,
                    onDismiss = { editingId = null },
                    onSaved   = { }
                )
            }


            vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
    }
}

