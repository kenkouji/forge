package com.forge.presentation.navigation

sealed class ForgeNavDestination(
    val route: String,
    val title: String
) {
    data object Home : ForgeNavDestination("home", "Home")
    data object Workouts : ForgeNavDestination("workouts", "Workouts")
    data object Progress : ForgeNavDestination("progress", "Progress")
    data object Journey : ForgeNavDestination("journey", "Journey")
    data object Profile : ForgeNavDestination("profile", "Profile")
    data object Nutrition : ForgeNavDestination("nutrition", "Nutrition")
    data object ActiveWorkout : ForgeNavDestination("active_workout", "Active Workout")
    data object ExerciseLibrary : ForgeNavDestination("exercise_library", "Exercise Library")
    data object Initialization : ForgeNavDestination("initialization", "Initialization")
    data object ExerciseDetail : ForgeNavDestination("exercise_detail/{exerciseId}", "Exercise Detail") {
        fun createRoute(exerciseId: String): String = "exercise_detail/$exerciseId"
    }

    companion object {
        val rootDestinations = listOf(Home, Workouts, Progress, Journey, Profile)
    }
}
