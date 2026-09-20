package com.forge.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forge.core.designsystem.component.ForgeAmbientBackground
import com.forge.domain.engine.AssistantService
import com.forge.presentation.exercise.ExerciseDetailScreen
import com.forge.presentation.exercise.ExerciseDetailViewModel
import com.forge.presentation.exercise.ExerciseLibraryScreen
import com.forge.presentation.exercise.ExerciseLibraryViewModel
import com.forge.presentation.home.HomeScreen
import com.forge.presentation.home.HomeViewModel
import com.forge.presentation.journey.JourneyScreen
import com.forge.presentation.journey.JourneyViewModel
import com.forge.presentation.nutrition.NutritionScreen
import com.forge.presentation.nutrition.NutritionViewModel
import com.forge.presentation.onboarding.InitializationViewModel
import com.forge.presentation.onboarding.InitializationWizard
import com.forge.presentation.profile.ProfileScreen
import com.forge.presentation.profile.ProfileViewModel
import com.forge.presentation.progress.ProgressScreen
import com.forge.presentation.progress.ProgressViewModel
import com.forge.presentation.workout.ActiveWorkoutScreen
import com.forge.presentation.workout.ActiveWorkoutViewModel
import com.forge.presentation.workout.WorkoutsScreen
import com.forge.presentation.workout.WorkoutsViewModel

@Composable
fun ForgeAppRoot(
    homeViewModel: HomeViewModel,
    workoutsViewModel: WorkoutsViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    exerciseLibraryViewModel: ExerciseLibraryViewModel,
    exerciseDetailViewModel: ExerciseDetailViewModel,
    progressViewModel: ProgressViewModel,
    journeyViewModel: JourneyViewModel,
    profileViewModel: ProfileViewModel,
    nutritionViewModel: NutritionViewModel,
    initializationViewModel: InitializationViewModel,
    assistantService: AssistantService? = null,
    isInitialized: Boolean = true,
    navController: NavHostController = rememberNavController(),
    hasActiveWorkout: Boolean = false,
    activeWorkoutTitle: String? = null,
    onResumeWorkoutClick: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isWorkoutActive = currentRoute == ForgeNavDestination.ActiveWorkout.route
    val showBottomBar = !isWorkoutActive &&
            currentRoute != ForgeNavDestination.Nutrition.route &&
            currentRoute != ForgeNavDestination.Initialization.route

    ForgeAmbientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (isInitialized) ForgeNavDestination.Home.route else ForgeNavDestination.Initialization.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Hard-gated 12-Step Initialization
                composable(ForgeNavDestination.Initialization.route) {
                    InitializationWizard(
                        viewModel = initializationViewModel,
                        onInitializationComplete = {
                            navController.navigate(ForgeNavDestination.Home.route) {
                                popUpTo(ForgeNavDestination.Initialization.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(ForgeNavDestination.Home.route) {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        assistantService = assistantService,
                        hasActiveWorkout = hasActiveWorkout,
                        activeWorkoutTitle = activeWorkoutTitle,
                        onStartWorkoutClick = {
                            if (hasActiveWorkout) {
                                navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                            } else {
                                activeWorkoutViewModel.startEmptyWorkout {
                                    navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                                }
                            }
                        },
                        onResumeWorkoutClick = {
                            navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                        },
                        onExploreExercisesClick = {
                            navController.navigate(ForgeNavDestination.ExerciseLibrary.route)
                        },
                        onLogWeightClick = {
                            navController.navigate(ForgeNavDestination.Profile.route)
                        },
                        onProgressPhotoClick = {
                            navController.navigate(ForgeNavDestination.Journey.route)
                        },
                        onViewAllProgressClick = {
                            navController.navigate(ForgeNavDestination.Progress.route)
                        }
                    )
                }

                composable(ForgeNavDestination.Workouts.route) {
                    WorkoutsScreen(
                        workoutsViewModel = workoutsViewModel,
                        onStartEmptyWorkout = {
                            activeWorkoutViewModel.startEmptyWorkout {
                                navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                            }
                        },
                        onOpenExerciseLibrary = {
                            navController.navigate(ForgeNavDestination.ExerciseLibrary.route)
                        }
                    )
                }

                composable(ForgeNavDestination.ActiveWorkout.route) {
                    ActiveWorkoutScreen(
                        viewModel = activeWorkoutViewModel,
                        onNavigateBack = {
                            navController.navigate(ForgeNavDestination.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }

                composable(ForgeNavDestination.ExerciseLibrary.route) {
                    ExerciseLibraryScreen(
                        viewModel = exerciseLibraryViewModel,
                        onExerciseClick = { exerciseId ->
                            navController.navigate(ForgeNavDestination.ExerciseDetail.createRoute(exerciseId))
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = ForgeNavDestination.ExerciseDetail.route,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val exerciseId = backStackEntry.arguments?.getString("exerciseId").orEmpty()
                    ExerciseDetailScreen(
                        exerciseId = exerciseId,
                        viewModel = exerciseDetailViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSiblingClick = { siblingId ->
                            navController.navigate(ForgeNavDestination.ExerciseDetail.createRoute(siblingId))
                        },
                        onAddToWorkout = { exercise ->
                            activeWorkoutViewModel.addExerciseToWorkout(exercise)
                            navController.navigate(ForgeNavDestination.ActiveWorkout.route) {
                                popUpTo(ForgeNavDestination.ActiveWorkout.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(ForgeNavDestination.Progress.route) {
                    ProgressScreen(viewModel = progressViewModel)
                }

                composable(ForgeNavDestination.Journey.route) {
                    JourneyScreen(viewModel = journeyViewModel)
                }

                composable(ForgeNavDestination.Profile.route) {
                    ProfileScreen(
                        profileViewModel = profileViewModel,
                        onNavigateToNutrition = {
                            navController.navigate(ForgeNavDestination.Nutrition.route)
                        }
                    )
                }

                composable(ForgeNavDestination.Nutrition.route) {
                    NutritionScreen(
                        viewModel = nutritionViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            // Floating Navigation Pill matching proto Layout.jsx
            if (showBottomBar) {
                ForgeBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { destination ->
                        if (destination == ForgeNavDestination.Home) {
                            navController.popBackStack(ForgeNavDestination.Home.route, inclusive = false)
                        }
                        navController.navigate(destination.route) {
                            popUpTo(ForgeNavDestination.Home.route) {
                                saveState = false
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
