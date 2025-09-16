package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AppUser(val uid: String, val name: String, val email: String)
data class AdminStat(val correct: Int, val total: Int) {
    val pct: Int get() = if (total > 0) (correct * 100) / total else 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsersScreen(onBack: () -> Unit) {
    val db = Firebase.firestore
    var users by remember { mutableStateOf(listOf<AppUser>()) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<AppUser?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            val snap = db.collection("users").get().await()
            users = snap.documents.map {
                AppUser(
                    uid = it.id,
                    name = it.getString("displayName").orEmpty(),
                    email = it.getString("email").orEmpty()
                )
            }.sortedBy { it.name.ifBlank { it.email } }
        }.onFailure { msg = it.message }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Korisnici") },
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

        Column(Modifier.fillMaxSize().padding(p)) {
            msg?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

            LazyColumn {
                items(users.size) { i ->
                    val u = users[i]
                    ListItem(
                        headlineContent = { Text(u.name.ifBlank { u.email }) },
                        supportingContent = { if (u.name.isNotBlank()) Text(u.email) },
                        trailingContent = {
                            TextButton(onClick = { opened = u }) { Text("Otvori") }
                        }
                    )
                    Divider()
                }
            }
        }

        opened?.let { user ->
            UserDetailsDialog(user = user, onClose = { opened = null })
        }
    }
}

@Composable
private fun UserDetailsDialog(user: AppUser, onClose: () -> Unit) {
    val db = Firebase.firestore
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    var displayName by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var photoUrl by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var lessons by remember { mutableStateOf(AdminStat(0, 0)) }
    var quizzes by remember { mutableStateOf(AdminStat(0, 0)) }
    var exams by remember { mutableStateOf(AdminStat(0, 0)) }

    LaunchedEffect(user.uid) {
        runCatching {
            val doc = db.collection("users").document(user.uid).get().await()
            displayName = doc.getString("displayName").orEmpty()
            email      = doc.getString("email").orEmpty()
            photoUrl   = doc.getString("photoUrl").orEmpty()
            role       = doc.getString("role").orEmpty()
            level      = doc.getString("level").orEmpty()

            var lC = 0; var lT = 0
            var qC = 0; var qT = 0
            var eC = 0; var eT = 0
            val roots = db.collection("users").document(user.uid).collection("progress").get().await().documents
            for (c in roots) {
                c.reference.collection("lessons").get().await().documents.let { ds ->
                    lT += ds.size; lC += ds.count { it.getBoolean("correct") == true }
                }
                c.reference.collection("quizzes").get().await().documents.let { ds ->
                    qT += ds.size; qC += ds.count { it.getBoolean("correct") == true }
                }
                c.reference.collection("exams").get().await().documents.let { ds ->
                    eT += ds.size; eC += ds.count { it.getBoolean("correct") == true }
                }
            }
            lessons = AdminStat(lC, lT)
            quizzes = AdminStat(qC, qT)
            exams   = AdminStat(eC, eT)
        }.onFailure { err = it.message }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Zatvori") } },
        title = { Text("Korisnik") },
        text = {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                    Text("Ime: $displayName")
                    if (email.isNotBlank()) Text("E-pošta: $email")
                    Text("UID: ${user.uid}")
                    if (role.isNotBlank()) Text("Uloga: $role")
                    if (level.isNotBlank()) Text("Razina: $level")

                    Spacer(Modifier.height(8.dp))
                    Text("Napredak", style = MaterialTheme.typography.titleSmall)
                    Text("Lekcije:  ${lessons.correct} / ${lessons.total} • ${lessons.pct}%")
                    Text("Kvizovi:  ${quizzes.correct} / ${quizzes.total} • ${quizzes.pct}%")
                    Text("Ispiti:   ${exams.correct} / ${exams.total} • ${exams.pct}%")

                    err?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error) }

                    Spacer(Modifier.height(8.dp))

                    val scope = rememberCoroutineScope()
                    var opMsg by remember { mutableStateOf<String?>(null) }
                    OutlinedButton(onClick = {
                        scope.launch {
                            opMsg = null
                            runCatching { resetUserProgressQuick(user.uid) }
                                .onSuccess { opMsg = "Progres resetiran." }
                                .onFailure { opMsg = "Greška: ${it.message}" }
                        }
                    }) { Text("Obriši progres") }
                    opMsg?.let { Text(it) }
                }
            }
        }
    )
}

private suspend fun resetUserProgressQuick(uid: String) {
    val db = Firebase.firestore
    val progressDocs = db.collection("users").document(uid).collection("progress").get().await()

    var batch = db.batch()
    var count = 0
    for (doc in progressDocs.documents) {
        batch.delete(doc.reference)
        count++
        if (count % 400 == 0) {
            batch.commit().await()
            batch = db.batch()
        }
    }
    if (count % 400 != 0) batch.commit().await()
}
