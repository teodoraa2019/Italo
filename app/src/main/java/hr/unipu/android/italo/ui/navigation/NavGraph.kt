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
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Info : Route("Info")
    data object Register : Route("register")
    data object Login : Route("login")
    data object AdminHome : Route("admin/home")
    data object Dashboard : Route("dashboard")
    data object Courses : Route("courses")
    data object LessonGroups : Route("course/{courseId}/groups")
    data object Profile : Route("profile")
    data object Progress : Route("progress")
    data object Faq : Route("faq")
    data object Dictionary : Route("dictionary")
    data object Menu : Route("menu")
    data object QuizList   : Route("quizzes/{courseId}")
    data object QuizGroups : Route("quizzes/{courseId}/{quizId}")
    data object QuizDetail : Route("quiz/{courseId}/{quizId}/{groupId}/{taskId}")
    data object ExamList   : Route("exams/{courseId}")
    data object ExamGroups : Route("exams/{courseId}/{examId}")
    data object ExamDetail : Route("exam/{courseId}/{examId}/{groupId}/{testId}")
    data object AdminLessonGroups : Route("admin/course/{level}/{courseId}/groups")
    data object AdminLessons      : Route("admin/lessons/{level}/{courseId}/{groupId}")
    data object AdminLessonDetail : Route("admin/lesson/{level}/{courseId}/{groupId}/{lessonId}")
    data object AdminQuizGroups : Route("admin/quiz/{quizId}/groups")
    data object AdminQuizzes    : Route("admin/quizzes/{quizId}/{groupId}")
    data object AdminQuizDetail : Route("admin/quiz/{quizId}/{groupId}/{taskId}")
    data object AdminExamGroups : Route("admin/exam/{examId}/groups")
    data object AdminExams      : Route("admin/exams/{examId}/{groupId}")
    data object AdminExamDetail : Route("admin/exam/{examId}/{groupId}/{testId}")
    data object AdminProfile : Route("admin/profile")
    data object AdminProfileSetup : Route("admin/profileSetup")
    data object AdminUsers : Route("admin/users")
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
                onRegisterSuccess = { nav.navigate("profileSetup") { popUpTo(0) } }
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    if (isAdmin) {
                        nav.navigate(Route.AdminHome.path) { popUpTo(0) }
                    } else {
                        nav.navigate(Route.Menu.path) { popUpTo(0) }
                    }
                },
                onGoToRegister = { nav.navigate(Route.Register.path) }
            )
        }

        composable(Route.Menu.path) {
            MenuScreen(
                onOpenCourse = { courseId -> nav.navigate("course/$courseId/groups") },
                onOpenQuizzes = { courseId -> nav.navigate("quizzes/$courseId") },
                onOpenExams   = { courseId -> nav.navigate("exams/$courseId") },
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

        composable("course/{courseId}/groups") { backStack ->
            val courseId = backStack.arguments?.getString("courseId")!!
            LessonGroupsScreen(
                courseId = courseId,
                onOpenGroup = { groupId -> nav.navigate("lessons/$courseId/$groupId") },
                onBack = { nav.popBackStack() }
            )
        }

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

        composable(
            route = Route.AdminLessonDetail.path,
            arguments = listOf(
                navArgument("level"){ type = NavType.StringType },
                navArgument("courseId"){ type = NavType.StringType },
                navArgument("groupId"){  type = NavType.StringType },
                navArgument("lessonId"){ type = NavType.StringType }
            )
        ) { back ->
            AdminLessonDetailScreen(
                level    = back.arguments!!.getString("level")!!,
                courseId = back.arguments!!.getString("courseId")!!,
                groupId  = back.arguments!!.getString("groupId")!!,
                lessonId = back.arguments!!.getString("lessonId")!!,
                onBack   = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.AdminLessons.path,
            arguments = listOf(
                navArgument("level"){ type = NavType.StringType },
                navArgument("courseId"){ type = NavType.StringType },
                navArgument("groupId"){ type = NavType.StringType }
            )
        ) { back ->
            val level = back.arguments!!.getString("level")!!
            val courseId = back.arguments!!.getString("courseId")!!
            val groupId  = back.arguments!!.getString("groupId")!!

            AdminLessonsScreen(
                level = level,
                courseId = courseId,
                groupId = groupId,
                onOpenLessonEdit = { lessonId ->
                    nav.navigate("admin/lesson/$level/$courseId/$groupId/$lessonId")
                },
                onBack = { nav.popBackStack() }
            )
        }


        composable(
            route = Route.AdminLessonGroups.path,
            arguments = listOf(
                navArgument("level"){ type = NavType.StringType },
                navArgument("courseId"){ type = NavType.StringType }
            )
        ) { back ->
            val level = back.arguments!!.getString("level")!!
            val courseId = back.arguments!!.getString("courseId")!!
            AdminLessonGroupsScreen(
                level = level,
                courseId = courseId,
                onOpenGroupAdmin = { lvl, cid, gid -> nav.navigate("admin/lessons/$lvl/$cid/$gid") },
                onBack = { nav.popBackStack() },
                vm = viewModel(factory = AdminLessonGroupsVM.factory(level, courseId))
            )
        }


        composable(Route.QuizList.path) { back ->
            val courseId = back.arguments!!.getString("courseId")!!
            QuizListScreen(
                courseId = courseId,
                onOpenQuiz = { quizId -> nav.navigate("quizzes/$courseId/$quizId") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.QuizGroups.path) { back ->
            val courseId = back.arguments!!.getString("courseId")!!
            val quizId   = back.arguments!!.getString("quizId")!!
            QuizGroupsScreen(
                courseId = courseId,
                quizId = quizId,
                onOpenGroup = { gid -> nav.navigate("quiz/$courseId/$quizId/$gid/first") }, // preskačemo kartice
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.QuizDetail.path,
            arguments = listOf(
                navArgument("courseId"){ type = NavType.StringType },
                navArgument("quizId"){   type = NavType.StringType },
                navArgument("groupId"){  type = NavType.StringType },
                navArgument("taskId"){   type = NavType.StringType }
            )
        ) { b ->
            QuizDetailScreen(
                courseId = b.arguments!!.getString("courseId")!!,
                quizId   = b.arguments!!.getString("quizId")!!,
                groupId  = b.arguments!!.getString("groupId")!!,
                taskId   = b.arguments!!.getString("taskId")!!,
                onBackToList = { nav.popBackStack() },
                onOpenTask = { id ->
                    nav.navigate("quiz/${b.arguments!!.getString("courseId")}/${b.arguments!!.getString("quizId")}/${b.arguments!!.getString("groupId")}/$id") {
                        popUpTo(Route.QuizDetail.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.AdminQuizGroups.path) { back ->
            val quizId = back.arguments!!.getString("quizId")!!
            AdminQuizGroupsScreen(
                quizId = quizId,
                onOpenGroupAdmin = { qid, gid -> nav.navigate("admin/quizzes/$qid/$gid") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.AdminQuizzes.path) { back ->
            val quizId  = back.arguments!!.getString("quizId")!!
            val groupId = back.arguments!!.getString("groupId")!!
            AdminQuizzesScreen(
                quizId = quizId,
                groupId = groupId,
                onOpenTaskEdit = { taskId -> nav.navigate("admin/quiz/$quizId/$groupId/$taskId") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.AdminQuizDetail.path,
            arguments = listOf(
                navArgument("quizId"){ type = NavType.StringType },
                navArgument("groupId"){ type = NavType.StringType },
                navArgument("taskId"){ type = NavType.StringType }
            )
        ) { back ->
            AdminQuizDetailScreen(
                quizId  = back.arguments!!.getString("quizId")!!,
                groupId = back.arguments!!.getString("groupId")!!,
                taskId  = back.arguments!!.getString("taskId")!!,
                onBack  = { nav.popBackStack() }
            )
        }

        composable(Route.ExamList.path) { back ->
            val courseId = back.arguments!!.getString("courseId")!!
            ExamListScreen(
                courseId = courseId,
                onOpenExam = { examId -> nav.navigate("exams/$courseId/$examId") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.ExamGroups.path) { back ->
            val courseId = back.arguments!!.getString("courseId")!!
            val examId   = back.arguments!!.getString("examId")!!
            ExamGroupsScreen(
                courseId = courseId,
                examId = examId,
                onOpenGroup = { gid -> nav.navigate("exam/$courseId/$examId/$gid/first") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.ExamDetail.path,
            arguments = listOf(
                navArgument("courseId"){ type = NavType.StringType },
                navArgument("examId"){   type = NavType.StringType },
                navArgument("groupId"){  type = NavType.StringType },
                navArgument("testId"){   type = NavType.StringType }
            )
        ) { b ->
            ExamDetailScreen(
                courseId = b.arguments!!.getString("courseId")!!,
                examId   = b.arguments!!.getString("examId")!!,
                groupId  = b.arguments!!.getString("groupId")!!,
                testId   = b.arguments!!.getString("testId")!!,
                onBackToList = { nav.popBackStack() },
                onOpenTest = { id ->
                    nav.navigate("exam/${b.arguments!!.getString("courseId")}/${b.arguments!!.getString("examId")}/${b.arguments!!.getString("groupId")}/$id") {
                        popUpTo(Route.ExamDetail.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.AdminHome.path) {
            AdminHomeScreen(
                onOpenLessons = { level, courseId -> nav.navigate("admin/course/$level/$courseId/groups") },
                onOpenQuizzes = { quizId   -> nav.navigate("admin/quiz/$quizId/groups") },
                onOpenExams   = { examId   -> nav.navigate("admin/exam/$examId/groups") }, // <—
                onOpenUsers   = { nav.navigate(Route.AdminUsers.path) },          // NOVO
                onOpenProfile = { nav.navigate(Route.AdminProfile.path) },        // NOVO
                onLogout      = { FirebaseAuth.getInstance().signOut(); nav.navigate(Route.Login.path){ popUpTo(0) } }
            )
        }

        composable(Route.AdminProfile.path) {
            AdminProfileScreen(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Route.AdminProfileSetup.path) },
                onOpenProgress = { nav.navigate(Route.AdminUsers.path) } // umjesto Progress
            )
        }

        composable(Route.AdminProfileSetup.path) {
            AdminProfileSetupScreen(
                onDone = { nav.popBackStack() },
                onSkip = { nav.popBackStack() }
            )
        }

        composable(Route.AdminUsers.path) {
            AllUsersScreen(onBack = { nav.popBackStack() })
        }

        composable(Route.AdminExamGroups.path) { back ->
            val examId = back.arguments!!.getString("examId")!!
            AdminExamGroupsScreen(
                examId = examId,
                onOpenGroupAdmin = { eid, gid -> nav.navigate("admin/exams/$eid/$gid") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.AdminExams.path) { back ->
            val examId  = back.arguments!!.getString("examId")!!
            val groupId = back.arguments!!.getString("groupId")!!
            AdminExamsScreen(
                examId = examId,
                groupId = groupId,
                onOpenTestEdit = { testId -> nav.navigate("admin/exam/$examId/$groupId/$testId") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.AdminExamDetail.path,
            arguments = listOf(
                navArgument("examId"){  type = NavType.StringType },
                navArgument("groupId"){ type = NavType.StringType },
                navArgument("testId"){  type = NavType.StringType }
            )
        ) { back ->
            AdminExamDetailScreen(
                examId  = back.arguments!!.getString("examId")!!,
                groupId = back.arguments!!.getString("groupId")!!,
                testId  = back.arguments!!.getString("testId")!!,
                onBack  = { nav.popBackStack() }
            )
        }

        // Profil
        composable(Route.Profile.path) {
            ProfileScreen(
                onBack = { nav.navigate(Route.Menu.path) },
                onEdit = { nav.navigate("profileSetup") },
                onOpenProgress = { nav.navigate(Route.Progress.path) }   // ← NOVO
            )
        }

        composable(Route.Progress.path) {
            ProgressScreen(onBack = { nav.popBackStack() })
        }


        composable("profileSetup") {
            UserProfileSetupScreen(
                onDone = { nav.navigate(Route.Profile.path) { popUpTo(0) } },
                onSkip = { nav.navigate(Route.Menu.path) { popUpTo(0) } }
            )
        }

        // Rječnik i ČPP
        composable(Route.Dictionary.path) { DictionaryScreen(onBack = { nav.popBackStack() }) }
        composable(Route.Faq.path)        { FaqScreen(onBack = { nav.popBackStack() }) }
    }
}
