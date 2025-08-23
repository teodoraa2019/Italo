package hr.unipu.android.italo.data

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
data class CourseFS(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val level: String = "",
    val order: Int = 0
)

data class LessonFS(
    val id: String = "",
    val title: String = "",   // mapiramo iz polja "it"
    val content: String = "", // mapiramo iz polja "hr"
    val imageUrl: String = ""
)

class CoursesRepoFS {
    private val courses = Firebase.firestore.collection("courses")

    fun listenCourses(onOk: (List<CourseFS>) -> Unit, onErr: (Throwable)->Unit): ListenerRegistration =
        courses
            .orderBy("order")
            .addSnapshotListener { s,e ->
                if (e!=null) return@addSnapshotListener onErr(e)
                onOk(s?.documents?.map {
                    CourseFS(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        description = it.getString("description") ?: "",
                        level = it.getString("level") ?: ""
                    )
                }.orEmpty())
            }

    fun listenLessons(courseDocId: String, onOk:(List<LessonFS>)->Unit, onErr:(Throwable)->Unit): ListenerRegistration =
        courses.document(courseDocId).collection("lessons")
            /* .orderBy("order")  // makni jer ga nema u bazi */
            .addSnapshotListener { s,e ->
                if (e!=null) return@addSnapshotListener onErr(e)
                onOk(s?.documents?.map { d ->
                    LessonFS(
                        id = d.id,
                        title = d.getString("it") ?: "",       // <-- it
                        content = d.getString("hr") ?: "",     // <-- hr
                        imageUrl = d.getString("imageUrl") ?: ""
                    )
                }.orEmpty())
            }
}
