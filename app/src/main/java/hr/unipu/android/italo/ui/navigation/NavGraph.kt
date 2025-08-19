package hr.unipu.android.italo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.unipu.android.italo.ui.screens.*

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Register : Route("register")
    data object Login : Route("login")
    data object Home : Route("home")
    data object Courses : Route("courses")
    data object Lessons : Route("lessons/{courseId}")
    data object Lesson  : Route("lesson/{lessonId}")
}

@Composable
fun ItaloNavGraph(
    nav: NavHostController = rememberNavController()
) {
    NavHost(navController = nav, startDestination = Route.Splash.path) {

        composable(Route.Splash.path) {
            SplashScreen(
                onFinished = { nav.navigate(Route.Register.path) { popUpTo(0) } }
            )
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onGoToLogin = { nav.navigate(Route.Login.path) },
                onRegisterSuccess = { nav.navigate(Route.Home.path) { popUpTo(0) } }
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = { nav.navigate(Route.Home.path) { popUpTo(0) } },
                onGoToRegister = { nav.navigate(Route.Register.path) }
            )
        }

        composable(Route.Home.path) { HomeScreen(onOpenCourses = { nav.navigate("courses") }) }

        composable(Route.Courses.path) {
            CoursesScreen(onOpenCourse = { id -> nav.navigate("lessons/$id") })
        }
        composable(Route.Lessons.path) { backStack ->
            val courseId = backStack.arguments?.getString("courseId")?.toInt() ?: 1
            LessonsScreen(
                courseId = courseId,
                onOpenLesson = { lessonId -> nav.navigate("lesson/$lessonId") }
            )
        }
        composable(Route.Lesson.path) { backStack ->
            val lessonId = backStack.arguments?.getString("lessonId")!!
            LessonDetailScreen(
                lessonId = lessonId,
                onNext = { nextId -> nav.navigate("lesson/$nextId") { popUpTo("lesson/$lessonId") { inclusive = true } } },
                onBackToList = { nav.popBackStack() }
            )
        }

    }
}
