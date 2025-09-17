package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun AdminQuizDetailScreen(
    quizId: String,
    groupId: String,
    taskId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var optionsText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(quizId, groupId, taskId) {
        try {
            val snap = db.collection("quizzes_a1").document(quizId)
                .collection(groupId).document(taskId).get().await()
            question   = snap.getString("question") ?: ""
            answer     = snap.getString("answer") ?: ""
            optionsText = (snap.get("options") as? List<String>)?.joinToString(", ") ?: ""
            imageUrl   = snap.getString("imageUrl") ?: ""
        } catch (e: Exception) {
            msg = "Greška: ${e.message}"
        } finally { loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uredi zadatak") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                label = { Text("Pitanje (question)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = answer, onValueChange = { answer = it },
                label = { Text("Točan odgovor (answer)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = optionsText, onValueChange = { optionsText = it },
                label = { Text("Opcije (odvojene zarezom)") }, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it },
                label = { Text("URL slike (opcionalno)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Pregled slike",
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            saving = true
                            val ref = db.collection("quizzes_a1").document(quizId)
                                .collection(groupId).document(taskId)

                            val optionsList = optionsText
                                .split(",").map { it.trim() }.filter { it.isNotEmpty() }

                            val data = mapOf(
                                "question" to question,
                                "answer" to answer,
                                "options" to optionsList,
                                "imageUrl" to imageUrl
                            )
                            ref.set(data, SetOptions.merge()).await()
                            msg = "✅ Spremljeno."
                        } catch (e: Exception) {
                            msg = "❌ Greška: ${e.message}"
                        } finally { saving = false }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Spremam..." else "Spremi") }

            msg?.let {
                Text(
                    it,
                    color = if (it.startsWith("❌")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@Composable
fun EditableTaskDialog(
    quizId: String,
    groupId: String,
    taskId: String?,   // može biti null za novi zadatak
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var optionsText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(quizId, groupId, taskId) {
        if (taskId != null) {
            try {
                val snap = db.collection("quizzes_a1").document(quizId)
                    .collection(groupId).document(taskId).get().await()
                question   = snap.getString("question") ?: ""
                answer     = snap.getString("answer") ?: ""
                optionsText = (snap.get("options") as? List<String>)?.joinToString(", ") ?: ""
                imageUrl   = snap.getString("imageUrl") ?: ""
            } finally { loading = false }
        } else {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Uredi zadatak") },
        text = {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = question, onValueChange = { question = it },
                        label = { Text("Pitanje") }
                    )
                    OutlinedTextField(
                        value = answer, onValueChange = { answer = it },
                        label = { Text("Točan odgovor") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = optionsText, onValueChange = { optionsText = it },
                        label = { Text("Opcije (zarezom odvojene)") }
                    )
                    OutlinedTextField(
                        value = imageUrl, onValueChange = { imageUrl = it },
                        label = { Text("URL slike") }, singleLine = true
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
                    val optionsList = optionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    try {
                        if (taskId == null) {
                            // Novi zadatak
                            val (newId, newOrder) = generateNextTaskIdAndOrder(quizId, groupId)
                            val data = mapOf(
                                "question" to question,
                                "answer" to answer,
                                "options" to optionsList,
                                "imageUrl" to imageUrl,
                                "order" to newOrder
                            )
                            db.collection("quizzes_a1").document(quizId)
                                .collection(groupId).document(newId).set(data).await()
                        } else {
                            // Update postojećeg
                            db.collection("quizzes_a1").document(quizId)
                                .collection(groupId).document(taskId)
                                .set(
                                    mapOf(
                                        "question" to question,
                                        "answer" to answer,
                                        "options" to optionsList,
                                        "imageUrl" to imageUrl
                                    ),
                                    SetOptions.merge()
                                ).await()
                        }
                        onSaved(); onDismiss()
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

                if (taskId != null) {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            scope.launch {
                                try {
                                    saving = true
                                    db.collection("quizzes_a1").document(quizId)
                                        .collection(groupId).document(taskId)
                                        .delete().await()
                                    msg = "Zadatak obrisan."
                                    onSaved(); onDismiss()
                                } catch (e: Exception) {
                                    msg = "Greška pri brisanju: ${e.message}"
                                } finally { saving = false }
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
