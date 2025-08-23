package hr.unipu.android.italo.data

data class Course(val id: Int, val titleHr: String, val titleIt: String)

data class Lesson(
    val id: String,
    val courseId: Int,
    val it: String,
    val hr: String,
    val imageUrl: String? = null
)

data class Quiz(val id: Int, val title: String)
