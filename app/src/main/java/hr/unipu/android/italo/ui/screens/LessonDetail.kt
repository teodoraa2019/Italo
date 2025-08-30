package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import hr.unipu.android.italo.data.LessonsVM
import com.google.firebase.firestore.FieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    groupId: String,
    courseId: String,
    lessonId: String,
    onBackToList: (Boolean) -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val vm: LessonsVM = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LessonsVM.factory(groupId = groupId, courseId = courseId)
    )
    val scope = rememberCoroutineScope()

    val items = vm.items
    if (items.isEmpty()) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("POVRATAK NA MENU") },
                navigationIcon = { IconButton(onClick = { onBackToList(false) }) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val lesson = items.firstOrNull { it.id == lessonId } ?: run {
        AssistiveError("Lekcija nije pronađena"); return
    }

    val all = items
    val curIndex = all.indexOfFirst { it.id == lessonId }
    val prev = all.getOrNull(curIndex - 1)
    val next = all.getOrNull(curIndex + 1)

    var locked by remember(lesson.id) { mutableStateOf(false) }
    var wasCorrect by remember(lesson.id) { mutableStateOf<Boolean?>(null) }
    var answer by remember(lesson.id) { mutableStateOf("") }
    val target = remember(lesson.id) { (lesson.title ?: "").trim().lowercase() }

    LaunchedEffect(courseId, groupId, lesson.id) {
        val uid = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
        val db = Firebase.firestore
        val docRef = db.collection("users").document(uid)
            .collection("progress").document(courseId)
            .collection("lessons").document("${groupId}__${lesson.id}")

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
            navigationIcon = { IconButton(onClick = { onBackToList(true) }) { Icon(Icons.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(lesson.imageUrl).build(),
                contentDescription = lesson.title,
                modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.extraLarge)
            )

            Text(lesson.content, style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = answer,
                onValueChange = { if (!locked) answer = it },
                enabled = !locked,
                label = { Text("Upiši riječ na talijanskom") },
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

            Spacer(Modifier.height(8.dp))

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
                        val lessonRef = courseRef.collection("lessons")
                            .document("${groupId}__${lessonId}")
                        val statsRef = courseRef.collection("meta").document("stats")

                        courseRef.set(mapOf("exists" to true), SetOptions.merge()).await()
                        lessonRef.set(
                            mapOf(
                                "attempted" to true,
                                "answer" to raw,
                                "correct" to isCorrect,
                                "groupId" to groupId,
                                "lessonId" to lessonId
                            ),
                            SetOptions.merge()
                        ).await()

                        db.runTransaction { tx ->
                            val statsSnap = tx.get(statsRef)
                            val total = (statsSnap.getLong("total") ?: 0L) + 1
                            var correct = (statsSnap.getLong("correct") ?: 0L)
                            val alreadyCorrect = (tx.get(lessonRef).getBoolean("correct") ?: false)
                            if (isCorrect && !alreadyCorrect) correct += 1
                            tx.set(statsRef, mapOf("total" to total, "correct" to correct), SetOptions.merge())
                        }.await()
                    }
                },
                enabled = !locked
            ) { Text("Potvrdi") }

            if (wasCorrect == false) {
                Spacer(Modifier.height(6.dp))
                Text("Točan odgovor: ${lesson.title}", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(enabled = prev != null, onClick = { prev?.let { onOpenLesson(it.id) } }) {
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
                            onClick = { onOpenLesson(all[i].id) },
                            label = { Text("${i + 1}", maxLines = 1, softWrap = false) },
                            modifier = Modifier.widthIn(min = 44.dp),
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

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try { markGroupCompleted(courseId, groupId) } catch (_: Exception) {}
                        onBackToList(true)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) { Text("Završi") }
        }
    }
}

suspend fun markGroupCompleted(courseId: String, groupId: String) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val ref = db.collection("users").document(uid)
        .collection("progress").document(courseId)

    ref.update("groups", FieldValue.arrayUnion(groupId))
        .addOnFailureListener {
            ref.set(mapOf("groups" to listOf(groupId)), SetOptions.merge())
        }.await()
}

