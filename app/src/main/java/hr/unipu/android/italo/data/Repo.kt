package hr.unipu.android.italo.data

object Repo {
    private val coursesSeed = listOf(
        Course(1, "Pića", "Bevande"),
        Course(2, "Životinje", "Animali"),
        Course(3, "Voće", "Frutta"),
    )

    // Dovoljno za start (dodaj još po istom patternu)
    private val lessonsSeed = listOf(
        Lesson("Lekcija_1_1", 1, "caffè", "kava", "https://i.imgur.com/0n3D9dU.jpeg"),
        Lesson("Lekcija_1_2", 1, "tè", "čaj", "https://i.imgur.com/0l1yC0x.jpeg"),
        Lesson("Lekcija_1_3", 1, "birra", "pivo", "https://i.imgur.com/0qMZ2fB.jpeg"),
        Lesson("Lekcija_1_4", 1, "acqua minerale", "mineralna voda", "https://i.imgur.com/M2Y8z1h.jpeg"),

        Lesson("Lekcija_2_1", 2, "leone", "lav", "https://i.imgur.com/g8Gd9nT.jpeg"),
        Lesson("Lekcija_2_2", 2, "elefante", "slon", "https://i.imgur.com/1b1n6mF.jpeg"),
        Lesson("Lekcija_2_3", 2, "zebra", "zebra", "https://i.imgur.com/GxB2j0K.jpeg"),

        Lesson("Lekcija_3_1", 3, "mela", "jabuka", "https://i.imgur.com/Zyqf0mE.jpeg"),
        Lesson("Lekcija_3_2", 3, "banana", "banana", "https://i.imgur.com/4V1Gk6R.jpeg"),
        Lesson("Lekcija_3_3", 3, "arancia", "naranča", "https://i.imgur.com/C5c9z2W.jpeg"),
    )

    fun getCourses(): List<Course> = coursesSeed
    fun getLessonsByCourse(courseId: Int): List<Lesson> =
        lessonsSeed.filter { it.courseId == courseId }
    fun getLesson(lessonId: String): Lesson? =
        lessonsSeed.firstOrNull { it.id == lessonId }
    fun getNextInCourse(currentId: String): Lesson? {
        val cur = getLesson(currentId) ?: return null
        val all = getLessonsByCourse(cur.courseId)
        val idx = all.indexOfFirst { it.id == currentId }
        return all.getOrNull(idx + 1)
    }
}
