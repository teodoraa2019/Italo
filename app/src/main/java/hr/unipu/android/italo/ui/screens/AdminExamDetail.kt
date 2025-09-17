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
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamDetailScreen(
    examId: String,
    groupId: String,
    testId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var answer   by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(true) }
    var saving   by remember { mutableStateOf(false) }
    var msg      by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(examId, groupId, testId) {
        try {
            val snap = db.collection("exams_a1").document(examId)
                .collection(groupId).document(testId).get().await()
            question = snap.getString("question") ?: ""
            answer   = snap.getString("answer") ?: ""
            imageUrl = snap.getString("imageUrl") ?: ""
        } catch (e: Exception) {
            msg = "Greška pri učitavanju: ${e.message}"
        } finally { loading = false }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Uredi test") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { p ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Naslov (it)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Sadržaj (hr)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL slike") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Pregled slike", modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            saving = true
                            val ref = db.collection("exams_a1").document(examId)
                                .collection(groupId).document(testId)
                            val data = mapOf("question" to question, "answer" to answer, "imageUrl" to imageUrl)
                            ref.set(data, SetOptions.merge()).await()
                            msg = "Spremljeno."
                        } catch (e: Exception) {
                            msg = "Greška pri spremanju: ${e.message}"
                        } finally { saving = false }
                    }
                },
                enabled = !saving, modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Spremam..." else "Spremi") }

            msg?.let {
                Text(it, color = if (it.startsWith("Greška")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@Composable
fun EditableExamTestDialog(
    examId: String,
    groupId: String,
    testId: String?,   // može biti null za novi test
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var answer   by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(true) }
    var saving   by remember { mutableStateOf(false) }
    var msg      by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(examId, groupId, testId) {
        if (testId != null) {
            try {
                val snap = db.collection("exams_a1").document(examId)
                    .collection(groupId).document(testId).get().await()
                question = snap.getString("question") ?: ""
                answer   = snap.getString("answer") ?: ""
                imageUrl = snap.getString("imageUrl") ?: ""
            } finally { loading = false }
        } else {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Uredi test") },
        text = {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Pitanje") })
                    OutlinedTextField(value = answer,   onValueChange = { answer = it },   label = { Text("Odgovor") }, singleLine = true)
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL slike") }, singleLine = true)

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
                        if (testId == null) {
                            // Novi test
                            val (newId, newOrder) = generateNextTestIdAndOrder(examId, groupId)
                            val data = mapOf(
                                "question" to question,
                                "answer" to answer,
                                "imageUrl" to imageUrl,
                                "order" to newOrder
                            )
                            db.collection("exams_a1").document(examId)
                                .collection(groupId).document(newId).set(data).await()
                        } else {
                            // Update postojećeg
                            db.collection("exams_a1").document(examId)
                                .collection(groupId).document(testId)
                                .set(mapOf("question" to question, "answer" to answer, "imageUrl" to imageUrl), SetOptions.merge())
                                .await()
                        }
                        onSaved(); onDismiss()
                    } catch (e: Exception) { msg = "Greška: ${e.message}" }
                    finally { saving = false }
                }
            }) { Text(if (saving) "Spremam..." else "Spremi") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = onDismiss) { Text("Odustani") }

                if (testId != null) {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            scope.launch {
                                try {
                                    saving = true
                                    db.collection("exams_a1").document(examId)
                                        .collection(groupId).document(testId)
                                        .delete().await()
                                    msg = "Test obrisan."
                                    onSaved(); onDismiss()
                                } catch (e: Exception) {
                                    msg = "Greška pri brisanju: ${e.message}"
                                } finally { saving = false }
                            }
                        }
                    ) { Text("Obriši", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
}
