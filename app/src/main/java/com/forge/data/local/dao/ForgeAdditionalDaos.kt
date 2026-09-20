package com.forge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forge.data.local.entity.AppSettingsEntity
import com.forge.data.local.entity.DailyActivityEntity
import com.forge.data.local.entity.TemplateExerciseEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.TransformationCheckInEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET initialization_step = :step, updated_at = :now WHERE id = 1")
    suspend fun updateInitializationStep(step: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET is_initialized = :isInitialized, updated_at = :now WHERE id = 1")
    suspend fun setInitialized(isInitialized: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}

@Dao
interface TrainingScheduleDao {
    @Query("SELECT * FROM training_schedule ORDER BY day_of_week ASC")
    fun getSchedule(): Flow<List<TrainingScheduleEntity>>

    @Query("SELECT * FROM training_schedule ORDER BY day_of_week ASC")
    suspend fun getScheduleSync(): List<TrainingScheduleEntity>

    @Query("SELECT * FROM training_schedule WHERE day_of_week = :dayOfWeek LIMIT 1")
    fun getScheduleForDay(dayOfWeek: Int): Flow<TrainingScheduleEntity?>

    @Query("SELECT * FROM training_schedule WHERE day_of_week = :dayOfWeek LIMIT 1")
    suspend fun getScheduleForDaySync(dayOfWeek: Int): TrainingScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(schedule: List<TrainingScheduleEntity>)

    @Update
    suspend fun updateDay(day: TrainingScheduleEntity)

    @Query("DELETE FROM training_schedule")
    suspend fun deleteAll()
}

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY created_at DESC")
    fun getTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id LIMIT 1")
    fun getTemplateById(id: String): Flow<WorkoutTemplateEntity?>

    @Query("SELECT * FROM workout_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateByIdSync(id: String): WorkoutTemplateEntity?

    @Query("SELECT * FROM template_exercises WHERE template_id = :templateId ORDER BY order_index ASC")
    fun getTemplateExercises(templateId: String): Flow<List<TemplateExerciseEntity>>

    @Query("SELECT * FROM template_exercises WHERE template_id = :templateId ORDER BY order_index ASC")
    suspend fun getTemplateExercisesSync(templateId: String): List<TemplateExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    @Query("DELETE FROM template_exercises WHERE template_id = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("DELETE FROM workout_templates")
    suspend fun deleteAllTemplates()

    @Query("DELETE FROM template_exercises")
    suspend fun deleteAllTemplateExercises()

    @Transaction
    suspend fun replaceTemplateWithExercises(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>
    ) {
        insertTemplate(template)
        deleteTemplateExercises(template.id)
        insertTemplateExercises(exercises)
    }
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    fun getActivityForDate(date: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getActivityForDateSync(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY date ASC")
    suspend fun getAllActivitiesSync(): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()
}

@Dao
interface TransformationDao {
    @Query("SELECT * FROM transformation_checkins ORDER BY week_number ASC")
    fun getCheckIns(): Flow<List<TransformationCheckInEntity>>

    @Query("SELECT * FROM transformation_checkins ORDER BY week_number ASC")
    suspend fun getCheckInsSync(): List<TransformationCheckInEntity>

    @Query("SELECT * FROM transformation_checkins WHERE week_number = :weekNumber LIMIT 1")
    fun getCheckInForWeek(weekNumber: Int): Flow<TransformationCheckInEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(checkIn: TransformationCheckInEntity)

    @Query("DELETE FROM transformation_checkins WHERE id = :id")
    suspend fun deleteCheckIn(id: String)

    @Query("DELETE FROM transformation_checkins")
    suspend fun deleteAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)
}
