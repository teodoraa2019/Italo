package hr.unipu.android.italo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.unipu.android.italo.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Info : Route("Info")
    data object Register : Route("register")
    data object Login : Route("login")
    data object Dashboard : Route("dashboard")
    data object Courses : Route("courses")
    data object LessonGroups : Route("course/{courseId}/groups")
    data object Profile : Route("profile")
    data object Faq : Route("faq")
    data object Dictionary : Route("dictionary")
    data object Menu : Route("menu")
}

@Composable
fun ItaloNavGraph(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Route.Splash.path) {

        composable(Route.Splash.path) {
            SplashScreen(onFinished = {
                nav.navigate(Route.Dashboard.path) { popUpTo(0) }
            })
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onGoToLogin = { nav.navigate(Route.Login.path) },
                onRegisterSuccess = { nav.navigate(Route.Menu.path) { popUpTo(0) } }
            )
        }
        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = { nav.navigate(Route.Menu.path) { popUpTo(0) } },
                onGoToRegister = { nav.navigate(Route.Register.path) }
            )
        }

        composable(Route.Menu.path) {
            MenuScreen(
                onOpenCourse = { courseId ->
                    nav.navigate("course/$courseId/groups")
                },
                onOpenQuiz   = { id /* Int */ -> nav.navigate("quiz/$id") },
                onOpenProfile = { nav.navigate(Route.Profile.path) },
                onLogout = { FirebaseAuth.getInstance().signOut(); nav.navigate(Route.Login.path){ popUpTo(0) } }
            )
        }

        composable(Route.Dashboard.path) {
            DashboardScreen(
                onOpenDictionary = { nav.navigate(Route.Dictionary.path) },
                onOpenInfo = { nav.navigate(Route.Info.path) },
                onOpenLogin = { nav.navigate(Route.Login.path) }
            )
        }

        composable(Route.Info.path) {
            InfoScreen(
                onBack = { nav.popBackStack() },
                onFaq  = { nav.navigate(Route.Faq.path) }
            )
        }

        // Tečajevi
        composable(Route.Courses.path) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                LoginRequiredScreen(
                    onGoToLogin = { nav.navigate(Route.Login.path) },
                    onBack = { nav.popBackStack() }
                )
            } else {
                CoursesScreen(onOpenCourse = { id: String -> nav.navigate("lessons/$id") })
            }
        }

        composable(Route.LessonGroups.path) { backStack ->
            val courseId = backStack.arguments?.getString("courseId")!!
            LessonGroupsScreen(
                courseId = courseId,
                onOpenGroup = { groupId ->
                    nav.navigate("lessons/$courseId/$groupId")
                },
                onBack = { nav.popBackStack() }
            )
        }

        // GRUPE
        composable("course/{courseId}/groups") { backStack ->
            val courseId = backStack.arguments?.getString("courseId")!!
            LessonGroupsScreen(
                courseId = courseId,
                onOpenGroup = { groupId -> nav.navigate("lessons/$courseId/$groupId") },
                onBack = { nav.popBackStack() }
            )
        }

        // LEKCIJE
        composable("lessons/{courseId}/{groupId}") { backStack ->
            val courseId = backStack.arguments?.getString("courseId")!!
            val groupId  = backStack.arguments?.getString("groupId")!!
            LessonsScreen(
                courseId = courseId,
                groupId  = groupId,
                onOpenLesson = { lessonId ->
                    nav.navigate("lesson/$courseId/$groupId/$lessonId")
                },
                onBackToMenu = { nav.popBackStack() }
            )
        }

        // DETALJ LEKCIJE
        composable(
            route = "lesson/{courseId}/{groupId}/{lessonId}",
            arguments = listOf(
                navArgument("courseId"){ type = NavType.StringType },
                navArgument("groupId"){  type = NavType.StringType },
                navArgument("lessonId"){ type = NavType.StringType }
            )
        ) { back ->
            val courseId = back.arguments?.getString("courseId")!!
            val groupId  = back.arguments?.getString("groupId")!!
            val lessonId = back.arguments?.getString("lessonId")!!
            LessonDetailScreen(
                courseId = courseId,
                groupId  = groupId,
                lessonId = lessonId,
                onBackToList = { nav.popBackStack() },
                onOpenLesson = { id ->
                    nav.navigate("lesson/$courseId/$groupId/$id") {
                        popUpTo("lesson/$courseId/$groupId/$lessonId") { inclusive = true }
                    }
                }
            )
        }

        // Profil
        composable(Route.Profile.path) {
            ProfileScreen(
                onBack = { nav.navigate(Route.Menu.path) },
                onEdit = { nav.navigate("profileSetup") }
            )
        }
        composable("profileSetup") {
            UserProfileSetupScreen(onDone = { nav.navigate(Route.Profile.path) { popUpTo(0) } })
        }

        // Rječnik i ČPP
        composable(Route.Dictionary.path) { DictionaryScreen(onBack = { nav.popBackStack() }) }
        composable(Route.Faq.path)        { FaqScreen(onBack = { nav.popBackStack() }) }
    }
}
