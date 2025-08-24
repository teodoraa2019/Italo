package hr.unipu.android.italo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.firestore.ListenerRegistration
import hr.unipu.android.italo.data.CourseFS
import hr.unipu.android.italo.data.CoursesRepoFS
import hr.unipu.android.italo.data.LessonFS

class CoursesVM(private val repo: CoursesRepoFS = CoursesRepoFS()) : ViewModel() {
    var items by mutableStateOf<List<CourseFS>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    private var reg: ListenerRegistration? = null

    init { reg = repo.listenCourses({ items = it }, { error = it.message }) }

    override fun onCleared() { reg?.remove() }
}

class LessonsVM(
    private val courseId: String,
    private val groupId: String,
    private val repo: CoursesRepoFS = CoursesRepoFS()
) : ViewModel() {

    var items by mutableStateOf<List<LessonFS>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    private var reg: ListenerRegistration? = null

    init {
        reg = repo.listenLessons(
            courseDocId = courseId,
            groupId = groupId,
            onOk = { items = it },
            onErr = { error = it.message }
        )
    }

    override fun onCleared() { reg?.remove() }

    companion object {
        fun factory(courseId: String, groupId: String) = viewModelFactory {
            initializer { LessonsVM(courseId, groupId) }
        }
    }
}
