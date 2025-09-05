package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

data class ExamItem(
    val id: String,
    val question: String = "",
    val answer: String = "",
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    courseId: String,
    examId: String,
    groupId: String,
    testId: String,
    onBackToList: () -> Unit,
    onOpenTest: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember(groupId, examId) { mutableStateOf(listOf<ExamItem>()) }

    LaunchedEffect(examId, groupId) {
        val db = Firebase.firestore
        val col = db.collection("exams_a1").document(examId).collection(groupId)
        val snap = try { col.orderBy("order").get().await() } catch (_: Exception) { col.get().await() }
        items = snap.documents.map {
            ExamItem(
                id = it.id,
                question = it.getString("question") ?: "",
                answer = it.getString("answer") ?: "",
                imageUrl = it.getString("imageUrl") ?: ""
            )
        }
    }

    LaunchedEffect(items, testId) {
        if (testId == "first" && items.isNotEmpty()) onOpenTest(items.first().id)
    }

    val current = items.firstOrNull { it.id == testId }
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

    LaunchedEffect(courseId, examId, groupId, current.id) {
        val uid = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
        val db = Firebase.firestore
        val docRef = db.collection("users").document(uid)
            .collection("progress").document(courseId)
            .collection("exams").document("${examId}__${groupId}__${current.id}")

        val snap = docRef.get().await()
        if (snap.exists()) {
            locked = true
            wasCorrect = snap.getBoolean("correct")
            answer = snap.getString("answer") ?: ""
        } else {
            locked = false
            wasCorrect = null
            answer = ""
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("POVRATAK NA MENU") },
            navigationIcon = { IconButton(onClick = onBackToList) { Icon(Icons.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (current.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(current.imageUrl).build(),
                    contentDescription = current.question,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.extraLarge)
                )
            }

            Text(current.question, style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = answer,
                onValueChange = { if (!locked) answer = it },
                enabled = !locked,
                label = { Text("Upiši odgovor") },
                singleLine = true,
                trailingIcon = {
                    if (wasCorrect != null) {
                        if (wasCorrect == true) Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32))
                        else Icon(Icons.Default.Close, null, tint = Color(0xFFC62828))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor =
                    if (wasCorrect == true) Color(0xFF2E7D32)
                    else if (wasCorrect == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor =
                    if (wasCorrect == true) Color(0xFF2E7D32)
                    else if (wasCorrect == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch {
                        val raw = answer
                        val normalized = raw.trim().lowercase()
                        if (normalized.isEmpty()) return@launch

                        val isCorrect = normalized == target
                        wasCorrect = isCorrect
                        locked = true

                        val uid = Firebase.auth.currentUser?.uid ?: return@launch
                        val db = Firebase.firestore
                        val courseRef = db.collection("users").document(uid)
                            .collection("progress").document(courseId)
                        val itemRef = courseRef.collection("exams")
                            .document("${examId}__${groupId}__${current.id}")
                        val statsRef = courseRef.collection("meta").document("exam_stats")

                        courseRef.set(mapOf("exists" to true), SetOptions.merge()).await()
                        itemRef.set(
                            mapOf(
                                "attempted" to true,
                                "answer" to raw,
                                "correct" to isCorrect,
                                "examId" to examId,
                                "groupId" to groupId,
                                "itemId" to current.id,
                                "question" to current.question,
                                "expected" to current.answer
                            ),
                            SetOptions.merge()
                        ).await()

                        db.runTransaction { tx ->
                            val statsSnap = tx.get(statsRef)
                            val total = (statsSnap.getLong("total") ?: 0L) + 1
                            var correct = (statsSnap.getLong("correct") ?: 0L)
                            val alreadyCorrect = (tx.get(itemRef).getBoolean("correct") ?: false)
                            if (isCorrect && !alreadyCorrect) correct += 1
                            tx.set(statsRef, mapOf("total" to total, "correct" to correct), SetOptions.merge())
                        }.await()
                    }
                },
                enabled = !locked
            ) { Text("Potvrdi") }

            if (wasCorrect == false) {
                Spacer(Modifier.height(6.dp))
                Text("Točan odgovor: ${current.answer}", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            // Paginacija (ista kao na kvizu)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenTest(it.id) } }) {
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
                            onClick = { onOpenTest(all[i].id) },
                            label = { Text("${i + 1}", maxLines = 1, softWrap = false) },
                            modifier = Modifier.widthIn(min = 44.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                IconButton(enabled = next != null, onClick = { next?.let { onOpenTest(it.id) } }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeća")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try { markExamGroupCompleted(courseId, examId, groupId) } catch (_: Exception) {}
                        onBackToList()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) { Text("Završi") }
        }
    }
}

suspend fun markExamGroupCompleted(courseId: String, examId: String, groupId: String) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val ref = db.collection("users").document(uid)
        .collection("progress").document(courseId)

    val key = "exams_groups"
    val value = "$examId::$groupId"

    ref.update(key, FieldValue.arrayUnion(value))
        .addOnFailureListener {
            ref.set(mapOf(key to listOf(value)), SetOptions.merge())
        }.await()
}
