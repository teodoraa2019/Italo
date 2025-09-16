package hr.unipu.android.italo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdminProfileSetupScreen(
    onDone: () -> Unit,
    onSkip: () -> Unit,
    vm: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val current = Firebase.auth.currentUser
    var name by remember { mutableStateOf(TextFieldValue(current?.displayName.orEmpty())) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val existingPhotoUrl = current?.photoUrl?.toString()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) imageUri = uri }

    val loading by vm.loading
    val error by vm.error

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Postavi profil", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Korisničko ime") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            val previewModel: Any? = imageUri ?: existingPhotoUrl
            AsyncImage(
                model = previewModel,
                contentDescription = "Profilna slika",
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) { Text(if (imageUri == null) "Odaberi sliku" else "Promijeni sliku") }

            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                enabled = !loading && name.text.trim().length >= 2,
                onClick = {
                    vm.saveProfile(
                        name = name.text.trim(),
                        imageUri = imageUri,
                        onDone = onDone
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Spremi")
            }

            TextButton(
                enabled = !loading,
                onClick = onSkip
            ) { Text("Preskoči") }
        }
    }
}

class AdminProfileViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun saveProfile(name: String, imageUri: Uri?, onDone: () -> Unit) {
        val user = auth.currentUser ?: run { error.value = "Niste prijavljeni."; return }
        loading.value = true; error.value = null

        viewModelScope.launch {
            try {
                val photoUrl = imageUri?.let {
                    val ref = storage.reference.child("avatars/${user.uid}/profile.jpg")
                    ref.putFile(it).await()
                    ref.downloadUrl.await().toString()
                }

                val req = userProfileChangeRequest {
                    displayName = name
                    if (photoUrl != null) photoUri = Uri.parse(photoUrl)
                }
                user.updateProfile(req).await()
                user.reload().await()

                val data = hashMapOf(
                    "uid" to user.uid,
                    "displayName" to name,
                    "photoUrl" to (photoUrl ?: user.photoUrl?.toString().orEmpty()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(user.uid)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                loading.value = false
                onDone()
            } catch (e: Exception) {
                loading.value = false
                error.value = e.message ?: "Greška pri uploadu/slanju."
            }
        }
    }

    private suspend fun updateAll(name: String, photoUrl: String?, onDone: () -> Unit) {
        val user = auth.currentUser ?: return
        val req = userProfileChangeRequest {
            displayName = name
            if (photoUrl != null) photoUri = Uri.parse(photoUrl)
        }
        user.updateProfile(req).await()
        user.reload().await()

        user.updateProfile(req).addOnCompleteListener {
            val data = hashMapOf(
                "uid" to user.uid,
                "displayName" to name,
                "photoUrl" to (photoUrl ?: user.photoUrl?.toString().orEmpty()),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    loading.value = false
                    onDone()
                }
                .addOnFailureListener { e ->
                    loading.value = false
                    error.value = "Greška pri spremanju profila: ${e.message}"
                }
        }.addOnFailureListener {
            loading.value = false
            error.value = "Greška pri ažuriranju Auth profila."
        }
    }
}
