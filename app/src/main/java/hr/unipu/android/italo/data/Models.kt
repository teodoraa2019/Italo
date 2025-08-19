package hr.unipu.android.italo.data

data class Course(val id: Int, val titleHr: String, val titleIt: String)

data class Lesson(
    val id: String,          // npr. "Lekcija_1_1"
    val courseId: Int,       // 1=Pića, 2=Životinje, 3=Voće...
    val it: String,          // talijanski
    val hr: String,          // hrvatski
    val imageUrl: String? = null
)
