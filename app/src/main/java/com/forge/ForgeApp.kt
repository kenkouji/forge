package com.forge

import android.app.Application
import com.forge.data.local.DatabaseSeedLoader
import com.forge.data.local.DefaultExercises
import com.forge.data.local.ForgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForgeApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ForgeDatabase by lazy {
        ForgeDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        seedDefaultExercisesIfEmpty()
    }

    private fun seedDefaultExercisesIfEmpty() {
        applicationScope.launch {
            DatabaseSeedLoader.seedDatabaseIfEmpty(this@ForgeApp, database.exerciseDao())
        }
    }
}
