package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

data class QuizItem(
    val id: String,
    val question: String = "",
    val answer: String = "",
    val options: List<String> = emptyList(),
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    courseId: String,
    quizId: String,
    groupId: String,
    taskId: String,
    onBackToList: () -> Unit,
    onOpenTask: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember(groupId, quizId) { mutableStateOf(listOf<QuizItem>()) }

    LaunchedEffect(quizId, groupId) {
        val db = Firebase.firestore
        val userLevel = getUserLevel()
        val col = db.collection("quizzes_$userLevel").document(quizId).collection(groupId)
        val snap = try { col.orderBy("order").get().await() } catch (_: Exception) { col.get().await() }
        items = snap.documents.map {
            QuizItem(
                id = it.id,
                question = it.getString("question") ?: "",
                answer = it.getString("answer") ?: "",
                options = it.get("options") as? List<String> ?: emptyList(),
                imageUrl = it.getString("imageUrl") ?: ""
            )
        }
    }

    LaunchedEffect(items, taskId) {
        if (taskId == "first" && items.isNotEmpty()) onOpenTask(items.first().id)
    }

    val current = items.firstOrNull { it.id == taskId }
    if (current == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = { IconButton(onClick = onBackToList) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }) { Box(Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        return
    }

    val all = items
    val curIndex = items.indexOfFirst { it.id == current.id }
    val prev = items.getOrNull(curIndex - 1)
    val next = items.getOrNull(curIndex + 1)

    var locked by remember(current.id) { mutableStateOf(false) }
    var wasCorrect by remember(current.id) { mutableStateOf<Boolean?>(null) }
    var answer by remember(current.id) { mutableStateOf("") }
    val target = remember(current.id) { current.answer.trim().lowercase() }
    var selectedOption by remember(current.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId, quizId, groupId, current.id) {
        val uid = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
        val db = Firebase.firestore
        val docRef = db.collection("users").document(uid)
            .collection("progress").document(courseId)
            .collection("quizzes").document("${quizId}__${groupId}__${current.id}")

        val snap = docRef.get().await()
        if (snap.exists()) {
            locked = true
            wasCorrect = snap.getBoolean("correct")
            val savedAnswer = snap.getString("answer") ?: ""
            answer = savedAnswer
            selectedOption = savedAnswer
        } else {
            locked = false
            wasCorrect = null
            answer = ""
            selectedOption = null
        }

    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("POVRATAK NA MENU") },
            navigationIcon = { IconButton(onClick = onBackToList) { Icon(Icons.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (current.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = current.imageUrl,
                    contentDescription = current.question,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Fit
                )
            }


            Text(current.question, style = MaterialTheme.typography.headlineMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                current.options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = {
                                if (!locked) {
                                    selectedOption = option
                                }
                            }
                        )
                        Text(option, Modifier.padding(start = 8.dp))

                        if (locked) {
                            if (option == selectedOption && option != current.answer) {
                                Icon(Icons.Default.Close, contentDescription = "Netočno", tint = Color(0xFFC62828))
                            }
                            if (option == current.answer) {
                                Icon(Icons.Default.Check, contentDescription = "Točno", tint = Color(0xFF2E7D32))
                            }
                        }

                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val chosen = selectedOption ?: return@launch
                        val isCorrect = chosen == current.answer
                        wasCorrect = isCorrect
                        locked = true

                        val uid = Firebase.auth.currentUser?.uid ?: return@launch
                        val db = Firebase.firestore
                        val courseRef = db.collection("users").document(uid)
                            .collection("progress").document(courseId)
                        val itemRef = courseRef.collection("quizzes")
                            .document("${quizId}__${groupId}__${current.id}")

                        courseRef.set(mapOf("exists" to true), SetOptions.merge()).await()
                        itemRef.set(
                            mapOf(
                                "attempted" to true,
                                "answer" to chosen,
                                "correct" to isCorrect,
                                "quizId" to quizId,
                                "groupId" to groupId,
                                "itemId" to current.id,
                                "question" to current.question,
                                "expected" to current.answer
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                },
                enabled = !locked && selectedOption != null
            ) { Text("Potvrdi") }

            if (wasCorrect == false) {
                Spacer(Modifier.height(6.dp))
                Text("Točan odgovor: ${current.answer}", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenTask(it.id) } }) {
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
                            onClick = { onOpenTask(all[i].id) },
                            label = { Text("${i + 1}", maxLines = 1, softWrap = false) },
                            modifier = Modifier.widthIn(min = 44.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                IconButton(enabled = next != null, onClick = { next?.let { onOpenTask(it.id) } }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeća")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try { markQuizGroupCompleted(courseId, quizId, groupId) } catch (_: Exception) {}
                        onBackToList()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) { Text("Završi") }
        }
    }
}


suspend fun markQuizGroupCompleted(courseId: String, quizId: String, groupId: String) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val ref = db.collection("users").document(uid)
        .collection("progress").document(courseId)

    val key = "quizzes_groups"
    val value = "$quizId::$groupId"

    ref.update(key, FieldValue.arrayUnion(value))
        .addOnFailureListener {
            ref.set(mapOf(key to listOf(value)), SetOptions.merge())
        }.await()
}