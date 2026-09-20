package com.forge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.forge.data.local.dao.AppSettingsDao
import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.dao.WorkoutTemplateDao
import com.forge.data.local.entity.AppSettingsEntity
import com.forge.data.local.entity.DailyActivityEntity
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
import com.forge.data.local.entity.TemplateExerciseEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.TransformationCheckInEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import com.forge.data.local.entity.WorkoutTemplateEntity

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
        com.forge.data.local.entity.WeightLogEntity::class,
        UserProfileEntity::class,
        TrainingScheduleEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        DailyActivityEntity::class,
        TransformationCheckInEntity::class,
        AppSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ForgeDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun trainingScheduleDao(): TrainingScheduleDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun transformationDao(): TransformationDao
    abstract fun appSettingsDao(): AppSettingsDao

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
