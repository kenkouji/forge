package com.forge.presentation.navigation

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.forge.data.local.ForgeDatabase
import com.forge.data.repository.ExerciseRepositoryImpl
import com.forge.data.repository.WorkoutRepositoryImpl
import com.forge.presentation.exercise.ExerciseDetailViewModel
import com.forge.presentation.exercise.ExerciseLibraryViewModel
import com.forge.presentation.home.HomeViewModel
import com.forge.presentation.journey.JourneyViewModel
import com.forge.presentation.nutrition.NutritionViewModel
import com.forge.presentation.onboarding.InitializationViewModel
import com.forge.presentation.profile.ProfileViewModel
import com.forge.presentation.progress.ProgressViewModel
import com.forge.presentation.workout.ActiveWorkoutViewModel
import com.forge.presentation.workout.WorkoutsViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dbExecutor: ExecutorService
    private lateinit var database: ForgeDatabase
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var activeWorkoutViewModel: ActiveWorkoutViewModel
    private lateinit var exerciseLibraryViewModel: ExerciseLibraryViewModel
    private lateinit var exerciseDetailViewModel: ExerciseDetailViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var workoutsViewModel: WorkoutsViewModel
    private lateinit var progressViewModel: ProgressViewModel
    private lateinit var journeyViewModel: JourneyViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var nutritionViewModel: NutritionViewModel
    private lateinit var initializationViewModel: InitializationViewModel
    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dbExecutor = Executors.newSingleThreadExecutor()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(dbExecutor)
            .setTransactionExecutor(dbExecutor)
            .build()
        workoutRepository = WorkoutRepositoryImpl(database.workoutDao())
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())
        activeWorkoutViewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        exerciseLibraryViewModel = ExerciseLibraryViewModel(exerciseRepository)
        exerciseDetailViewModel = ExerciseDetailViewModel(exerciseRepository)

        val userProfileDao = database.userProfileDao()
        val trainingScheduleDao = database.trainingScheduleDao()
        val workoutTemplateDao = database.workoutTemplateDao()
        val dailyActivityDao = database.dailyActivityDao()
        val transformationDao = database.transformationDao()
        val appSettingsDao = database.appSettingsDao()
        val workoutDao = database.workoutDao()
        val exerciseDao = database.exerciseDao()

        homeViewModel = HomeViewModel(userProfileDao, trainingScheduleDao, workoutDao, dailyActivityDao, null)
        workoutsViewModel = WorkoutsViewModel(trainingScheduleDao, workoutTemplateDao, workoutDao, exerciseRepository)
        progressViewModel = ProgressViewModel(workoutRepository, exerciseRepository)
        val vaultManager = com.forge.domain.engine.TransformationVaultManager(context)
        journeyViewModel = JourneyViewModel(transformationDao, userProfileDao, vaultManager)
        profileViewModel = ProfileViewModel(userProfileDao, appSettingsDao, workoutDao, exerciseDao)
        nutritionViewModel = NutritionViewModel(userProfileDao, dailyActivityDao, workoutDao)
        initializationViewModel = InitializationViewModel(userProfileDao, trainingScheduleDao, workoutTemplateDao, vaultManager)

        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
        dbExecutor.shutdown()
    }

    private fun setContent(hasActiveWorkout: Boolean = false) {
        composeTestRule.setContent {
            com.forge.core.designsystem.theme.ForgeTheme {
                ForgeAppRoot(
                    homeViewModel = homeViewModel,
                    workoutsViewModel = workoutsViewModel,
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    exerciseLibraryViewModel = exerciseLibraryViewModel,
                    exerciseDetailViewModel = exerciseDetailViewModel,
                    progressViewModel = progressViewModel,
                    journeyViewModel = journeyViewModel,
                    profileViewModel = profileViewModel,
                    nutritionViewModel = nutritionViewModel,
                    initializationViewModel = initializationViewModel,
                    isInitialized = true,
                    navController = navController,
                    hasActiveWorkout = hasActiveWorkout
                )
            }
        }
    }

    @Test
    fun app_starts_at_home_destination() {
        setContent()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_to_workouts_and_back_to_home() {
        setContent()
        // 1. Home -> Workouts
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Workouts.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Workouts.route)

        // 2. Workouts -> Home
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_to_progress_and_back_to_home() {
        setContent()
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Progress.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Progress.route)

        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_to_journey_and_back_to_home() {
        setContent()
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Journey.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Journey.route)

        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_to_profile_and_back_to_home() {
        setContent()
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Profile.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Profile.route)

        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_to_home_reselection_maintains_home_at_root() {
        setContent()
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_while_active_workout_exists_allows_navigation_to_and_from_active_session() {
        setContent(hasActiveWorkout = true)
        // Navigate from Home -> ActiveWorkout
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.ActiveWorkout.route)
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.ActiveWorkout.route)

        // Minimize back to Home while workout remains active
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)
    }

    @Test
    fun home_after_completing_a_workout_navigates_cleanly_to_home() {
        setContent()
        // Navigate to active workout
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.ActiveWorkout.route)
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.ActiveWorkout.route)

        // Complete/save session triggers onNavigateBack to Home
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = false }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)

        // Subsequent bottom bar navigation still functions cleanly
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Workouts.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Workouts.route)
    }

    @Test
    fun home_after_discarding_a_workout_navigates_cleanly_to_home() {
        setContent()
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.ActiveWorkout.route)
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.ActiveWorkout.route)

        // Discard session triggers onNavigateBack to Home
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Home.route) {
                popUpTo(navController.graph.id) { saveState = false }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Home.route)

        // Verify Home remains the root
        composeTestRule.runOnUiThread {
            navController.navigate(ForgeNavDestination.Profile.route) {
                popUpTo(navController.graph.id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo(ForgeNavDestination.Profile.route)
    }
}
