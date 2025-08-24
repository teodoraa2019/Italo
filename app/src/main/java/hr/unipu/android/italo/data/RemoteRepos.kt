package hr.unipu.android.italo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CourseFS(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val level: String = "",
    val order: Int = 0
)

data class LessonFS(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = ""
)

class CoursesRepoFS {
    private val courses = Firebase.firestore.collection("courses")

    fun listenCourses(
        onOk: (List<CourseFS>) -> Unit,
        onErr: (Throwable) -> Unit
    ): ListenerRegistration =
        courses.orderBy("order").addSnapshotListener { s, e ->
            if (e != null) return@addSnapshotListener onErr(e)
            onOk(s?.documents?.map {
                CourseFS(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    description = it.getString("description") ?: "",
                    level = it.getString("level") ?: ""
                )
            }.orEmpty())
        }

    fun listenLessons(
        courseDocId: String,
        groupId: String,
        onOk: (List<LessonFS>) -> Unit,
        onErr: (Throwable) -> Unit
    ): ListenerRegistration =
        courses.document(courseDocId).collection(groupId)
            .addSnapshotListener { s, e ->
                if (e != null) return@addSnapshotListener onErr(e)
                onOk(s?.documents?.map { d ->
                    LessonFS(
                        id = d.id,
                        title = d.getString("it") ?: "",
                        content = d.getString("hr") ?: "",
                        imageUrl = d.getString("imageUrl") ?: ""
                    )
                }.orEmpty())
            }
}
class LessonsVM(
    private val courseId: String,
    private val groupId: String
) : ViewModel() {

    var items by mutableStateOf(listOf<LessonFS>()); private set
    var error by mutableStateOf<String?>(null);      private set

    init { load() }

    private fun load() = viewModelScope.launch {
        try {
            val snap = Firebase.firestore
                .collection("courses").document(courseId)
                .collection(groupId)
                .get().await()

            items = snap.documents.map { d ->
                LessonFS(
                    id = d.id,
                    title = d.getString("it") ?: "",
                    content = d.getString("hr") ?: "",
                    imageUrl = d.getString("imageUrl") ?: ""
                )
            }
        } catch (e: Exception) { error = e.message }
    }

    companion object {
        fun factory(courseId: String, groupId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LessonsVM(courseId, groupId) as T
                }
            }
    }
}
