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
import androidx.compose.runtime.mutableStateMapOf

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
    val imageUrl: String = "",
    val order: Int = 0
)
class CoursesRepoFS(private val userLevel: String) {
    private val courses = Firebase.firestore.collection("courses_$userLevel")

    fun listenCourses(
        onOk: (List<CourseFS>) -> Unit,
        onErr: (Throwable) -> Unit
    ): ListenerRegistration =
        courses.orderBy("order").addSnapshotListener { s, e ->
            if (e != null) return@addSnapshotListener onErr(e)
            onOk(
                s?.documents?.map {
                    CourseFS(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        description = it.getString("description") ?: "",
                        level = it.getString("level") ?: ""
                    )
                }.orEmpty()
            )
        }

    fun listenLessons(
        courseDocId: String,
        groupId: String,
        onOk: (List<LessonFS>) -> Unit,
        onErr: (Throwable) -> Unit
    ): ListenerRegistration =
        courses.document(courseDocId).collection(groupId)
            .orderBy("order")
            .addSnapshotListener { s, e ->
                if (e != null) return@addSnapshotListener onErr(e)
                onOk(
                    s?.documents?.map { d ->
                        LessonFS(
                            id = d.id,
                            title = d.getString("it") ?: "",
                            content = d.getString("hr") ?: "",
                            imageUrl = d.getString("imageUrl") ?: ""
                        )
                    }.orEmpty()
                )
            }
}
class LessonsVM(
    private val courseId: String,
    private val groupId: String,
    private val userLevel: String? = null
) : ViewModel() {

    var items by mutableStateOf(listOf<LessonFS>()); private set
    var error by mutableStateOf<String?>(null);      private set

    init { load() }

    private fun load() = viewModelScope.launch {
        try {
            val db = Firebase.firestore
            val levels = listOf("a1", "a2", "b1", "b2", "c1", "c2")

            val snaps = if (userLevel != null) {
                listOf(
                    db.collection("courses_$userLevel")
                        .document(courseId)
                        .collection(groupId)
                        .orderBy("order")
                        .get().await()
                )
            } else {
                levels.mapNotNull { lvl ->
                    try {
                        db.collection("courses_$lvl")
                            .document(courseId)
                            .collection(groupId)
                            .orderBy("order")
                            .get().await()
                    } catch (_: Exception) { null }
                }
            }

            items = snaps.flatMap { snap ->
                snap.documents.map { d ->
                    LessonFS(
                        id = d.id,
                        title = d.getString("it") ?: "",
                        content = d.getString("hr") ?: "",
                        imageUrl = d.getString("imageUrl") ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }

    companion object {
        fun factory(courseId: String, groupId: String, userLevel: String? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LessonsVM(courseId, groupId, userLevel) as T
                }
            }
    }
}
