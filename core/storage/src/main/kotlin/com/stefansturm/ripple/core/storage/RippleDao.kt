package com.stefansturm.ripple.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RippleDao {
    @Query("SELECT * FROM intakes WHERE dateMs >= :startMs AND dateMs < :endMs ORDER BY dateMs ASC, createdAtMs ASC, id ASC")
    fun observeIntakesBetween(startMs: Long, endMs: Long): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intakes ORDER BY dateMs ASC, createdAtMs ASC, id ASC")
    fun observeAllIntakes(): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intakes ORDER BY dateMs ASC, createdAtMs ASC, id ASC")
    suspend fun allIntakes(): List<IntakeEntity>

    @Query("SELECT * FROM intakes WHERE id = :id LIMIT 1")
    suspend fun findIntake(id: String): IntakeEntity?

    @Query("SELECT * FROM intakes WHERE isDeleted = 0 ORDER BY dateMs DESC, createdAtMs DESC, id DESC LIMIT 1")
    suspend fun findLatestActiveIntake(): IntakeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIntake(entity: IntakeEntity): Long

    @Update
    suspend fun updateIntake(entity: IntakeEntity)

    @Query("SELECT * FROM containers WHERE isDeleted = 0 ORDER BY sort ASC, createdAtMs ASC, id ASC")
    fun observeActiveContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers ORDER BY sort ASC, createdAtMs ASC, id ASC")
    suspend fun allContainers(): List<ContainerEntity>

    @Query("SELECT * FROM containers WHERE id = :id LIMIT 1")
    suspend fun findContainer(id: String): ContainerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContainer(entity: ContainerEntity)

    @Query("UPDATE containers SET isDefault = 0, updatedAtMs = :updatedAtMs WHERE isDeleted = 0")
    suspend fun clearContainerDefaults(updatedAtMs: Long)

    @Query("UPDATE containers SET isDefault = 1, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun setContainerDefault(id: String, updatedAtMs: Long)

    @Query("SELECT * FROM containers WHERE isDeleted = 0 ORDER BY sort ASC, createdAtMs ASC, id ASC LIMIT 1")
    suspend fun firstActiveContainer(): ContainerEntity?

    @Query("UPDATE containers SET isDeleted = 1, isDefault = 0, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun softDeleteContainer(id: String, updatedAtMs: Long)

    @Query("SELECT * FROM profile WHERE key = :key LIMIT 1")
    fun observeProfile(key: String = PROFILE_KEY): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE key = :key LIMIT 1")
    suspend fun findProfile(key: String = PROFILE_KEY): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: ProfileEntity)

    @Query("SELECT * FROM goal_settings WHERE key = :key LIMIT 1")
    fun observeGoal(key: String = GOAL_KEY): Flow<GoalEntity?>

    @Query("SELECT * FROM goal_settings WHERE key = :key LIMIT 1")
    suspend fun findGoal(key: String = GOAL_KEY): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(entity: GoalEntity)

    @Query("SELECT * FROM reminder_rule WHERE key = :key LIMIT 1")
    fun observeReminder(key: String = REMINDER_KEY): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminder_rule WHERE key = :key LIMIT 1")
    suspend fun findReminder(key: String = REMINDER_KEY): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(entity: ReminderEntity)
}
