package com.forge.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forge.presentation.exercise.ExerciseDetailScreen
import com.forge.presentation.exercise.ExerciseDetailViewModel
import com.forge.presentation.exercise.ExerciseLibraryScreen
import com.forge.presentation.exercise.ExerciseLibraryViewModel
import com.forge.presentation.home.HomeScreen
import com.forge.presentation.journey.JourneyScreen
import com.forge.presentation.profile.ProfileScreen
import com.forge.presentation.progress.ProgressScreen
import com.forge.presentation.progress.ProgressScreen
import com.forge.presentation.progress.ProgressViewModel
import com.forge.presentation.workout.ActiveWorkoutScreen
import com.forge.presentation.workout.ActiveWorkoutViewModel
import com.forge.presentation.workout.WorkoutsScreen

@Composable
fun ForgeAppRoot(
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    exerciseLibraryViewModel: ExerciseLibraryViewModel,
    exerciseDetailViewModel: ExerciseDetailViewModel,
    progressViewModel: ProgressViewModel? = null,
    navController: NavHostController = rememberNavController(),
    hasActiveWorkout: Boolean = false,
    activeWorkoutTitle: String? = null,
    onResumeWorkoutClick: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isWorkoutActive = currentRoute == ForgeNavDestination.ActiveWorkout.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar during active gym session for maximum focus and space
            if (!isWorkoutActive) {
                ForgeBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ForgeNavDestination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(ForgeNavDestination.Home.route) {
                HomeScreen(
                    hasActiveWorkout = hasActiveWorkout,
                    activeWorkoutTitle = activeWorkoutTitle,
                    onStartWorkoutClick = {
                        navController.navigate(ForgeNavDestination.Workouts.route)
                    },
                    onResumeWorkoutClick = {
                        navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                    },
                    onExploreExercisesClick = {
                        navController.navigate(ForgeNavDestination.ExerciseLibrary.route)
                    }
                )
            }
            composable(ForgeNavDestination.Workouts.route) {
                WorkoutsScreen(
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
                            restoreState = true
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
                JourneyScreen()
            }
            composable(ForgeNavDestination.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
