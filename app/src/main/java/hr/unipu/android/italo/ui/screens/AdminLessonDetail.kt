package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLessonDetailScreen(
    level: String,
    courseId: String,
    groupId: String,
    lessonId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var hrText by remember { mutableStateOf("") }
    var itText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId, groupId, lessonId) {
        try {
            val snap = db.collection("courses_$level").document(courseId)
                .collection(groupId).document(lessonId).get().await()
            hrText   = snap.getString("hr") ?: ""
            itText   = snap.getString("it") ?: ""
            imageUrl = snap.getString("imageUrl") ?: ""
        } catch (e: Exception) {
            msg = "Greška pri učitavanju: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uredi lekciju") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = itText, onValueChange = { itText = it },
                label = { Text("Naslov (it)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = hrText, onValueChange = { hrText = it },
                label = { Text("Sadržaj (hr)") }, minLines = 3, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it },
                label = { Text("URL slike") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Pregled slike",
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            saving = true
                            val ref = db.collection("courses_$level").document(courseId)
                                .collection(groupId).document(lessonId)

                            val data = mapOf(
                                "hr" to hrText,
                                "it" to itText,
                                "imageUrl" to imageUrl
                            )
                            ref.set(data, SetOptions.merge()).await()
                            msg = "Spremljeno."
                        } catch (e: Exception) {
                            msg = "Greška pri spremanju: ${e.message}"
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Spremam..." else "Spremi") }

            msg?.let {
                Text(
                    it,
                    color = if (it.startsWith("Greška")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EditableLessonDialog(
    level: String,
    courseId: String,
    groupId: String,
    lessonId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var hrText by remember { mutableStateOf("") }
    var itText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId, groupId, lessonId) {
        if (lessonId != null) {
            try {
                val snap = db.collection("courses_$level").document(courseId)
                    .collection(groupId).document(lessonId).get().await()
                hrText   = snap.getString("hr") ?: ""
                itText   = snap.getString("it") ?: ""
                imageUrl = snap.getString("imageUrl") ?: ""
            } finally { loading = false }
        } else {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Uredi lekciju") },
        text = {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itText, onValueChange = { itText = it },
                        label = { Text("Naslov (it)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hrText, onValueChange = { hrText = it },
                        label = { Text("Sadržaj (hr)") }, minLines = 3, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrl, onValueChange = { imageUrl = it },
                        label = { Text("URL slike") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                    msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving && !loading, onClick = {
                scope.launch {
                    saving = true
                    try {
                        if (lessonId == null) {
                            val (newId, newOrder) = generateNextLessonIdAndOrder(level, courseId, groupId)

                            val data = mapOf(
                                "hr" to hrText,
                                "it" to itText,
                                "imageUrl" to imageUrl,
                                "order" to newOrder
                            )

                            db.collection("courses_$level").document(courseId)
                                .collection(groupId).document(newId)
                                .set(data).await()
                        } else {
                            db.collection("courses_$level").document(courseId)
                                .collection(groupId).document(lessonId)
                                .set(
                                    mapOf("hr" to hrText, "it" to itText, "imageUrl" to imageUrl),
                                    SetOptions.merge()
                                ).await()
                        }

                        onSaved()
                        onDismiss()
                    } catch (e: Exception) {
                        msg = "Greška: ${e.message}"
                    } finally { saving = false }

                }
            }) { Text(if (saving) "Spremam..." else "Spremi") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = onDismiss) {
                    Text("Odustani")
                }

                if (lessonId != null) {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            scope.launch {
                                try {
                                    saving = true
                                    db.collection("courses_$level").document(courseId)
                                        .collection(groupId).document(lessonId)
                                        .delete().await()

                                    msg = "Lekcija obrisana."
                                    onSaved()
                                    onDismiss()
                                } catch (e: Exception) {
                                    msg = "Greška pri brisanju: ${e.message}"
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    ) {
                        Text("Obriši", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

    )
}


