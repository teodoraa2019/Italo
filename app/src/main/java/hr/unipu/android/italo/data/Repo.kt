package hr.unipu.android.italo.data

object Repo {
    private val coursesSeed = listOf(
        Course(1, "Pića", "Bevande"),
        Course(2, "Životinje", "Animali"),
        Course(3, "Voće", "Frutta"),
    )

    private val lessonsSeed = listOf(
        Lesson("Lekcija_1_1", 1, "caffè", "kava", "https://i.imgur.com/0n3D9dU.jpeg"),
        Lesson("Lekcija_1_2", 1, "tè", "čaj", "https://i.imgur.com/0l1yC0x.jpeg"),
        // ...
    )

    // ⬇️ Premjesti kvizove UNUTAR Repo
    private val quizzesSeed = listOf(
        Quiz(1, "Kviz 1 - osnove"),
        Quiz(2, "Kviz 2 - brojevi"),
        Quiz(3, "Test 1 - osnove"),
        Quiz(4, "Test 2 - brojevi")
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

    fun getQuizzes(): List<Quiz> = quizzesSeed   // ✅ sad postoji Repo.getQuizzes()
}
