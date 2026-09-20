package com.forge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseAliasEntity
import com.forge.data.local.entity.ExerciseAttributeEntity
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.ExerciseEquipmentEntity
import com.forge.data.local.entity.ExerciseFamilyEntity
import com.forge.data.local.entity.ExerciseFamilyMemberEntity
import com.forge.data.local.entity.ExerciseFtsEntity
import com.forge.data.local.entity.ExerciseMuscleEntity
import com.forge.data.local.entity.MuscleEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseFtsEntity::class,
        ExerciseAliasEntity::class,
        MuscleEntity::class,
        ExerciseMuscleEntity::class,
        EquipmentEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseAttributeEntity::class,
        ExerciseFamilyEntity::class,
        ExerciseFamilyMemberEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        com.forge.data.local.entity.ExerciseProgressionRecordEntity::class,
        com.forge.data.local.entity.ExercisePersonalRecordEntity::class,
        com.forge.data.local.entity.UserNutritionProfileEntity::class,
        com.forge.data.local.entity.WeightLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ForgeDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DATABASE_NAME = "forge.db"

        @Volatile
        private var INSTANCE: ForgeDatabase? = null

        fun getInstance(context: Context): ForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ForgeDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
