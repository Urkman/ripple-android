package com.stefansturm.ripple.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stefansturm.ripple.core.domain.ActivityLevel
import com.stefansturm.ripple.core.domain.Beverage
import com.stefansturm.ripple.core.domain.ClockTime
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.GoalMode
import com.stefansturm.ripple.core.domain.GoalSettings
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.VolumeUnit
import java.time.Instant

@Entity(tableName = "intakes")
data class IntakeEntity(
    @PrimaryKey val id: String,
    val dateMs: Long,
    val amountMl: Int,
    val beverage: String,
    val source: String,
    val containerId: String?,
    val note: String?,
    val isDeleted: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "containers")
data class ContainerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountMl: Int,
    val isDefault: Boolean,
    val sort: Int,
    val symbol: String,
    val isDeleted: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val key: String = PROFILE_KEY,
    val preferredUnit: String,
    val bodyMassKg: Int?,
    val activityLevel: String,
    val wakeHour: Int,
    val wakeMinute: Int,
    val sleepHour: Int,
    val sleepMinute: Int,
    val remindersEnabled: Boolean,
    val afterLastSipMinutes: Int,
    val healthConnectEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val onboardingComplete: Boolean,
    val updatedAtMs: Long
)

@Entity(tableName = "goal_settings")
data class GoalEntity(
    @PrimaryKey val key: String = GOAL_KEY,
    val mode: String,
    val manualGoalMl: Int,
    val updatedAtMs: Long
)

@Entity(tableName = "reminder_rule")
data class ReminderEntity(
    @PrimaryKey val key: String = REMINDER_KEY,
    val enabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val intervalMinutes: Int,
    val afterLastSipMinutes: Int,
    val updatedAtMs: Long
)

const val PROFILE_KEY = "profile"
const val GOAL_KEY = "goal"
const val REMINDER_KEY = "reminder"

fun IntakeEntity.toDomain(): Intake = Intake(
    id = id,
    date = Instant.ofEpochMilli(dateMs),
    amountMl = Milliliters(amountMl),
    beverage = Beverage.entries.firstOrNull { it.name.equals(beverage, ignoreCase = true) } ?: Beverage.WATER,
    source = IntakeSource.fromRaw(source),
    containerId = containerId,
    note = note,
    isDeleted = isDeleted,
    createdAt = Instant.ofEpochMilli(createdAtMs),
    updatedAt = Instant.ofEpochMilli(updatedAtMs)
)

fun Intake.toEntity(): IntakeEntity = IntakeEntity(
    id = id,
    dateMs = date.toEpochMilli(),
    amountMl = amountMl.value,
    beverage = beverage.name.lowercase(),
    source = source.name.lowercase(),
    containerId = containerId,
    note = note,
    isDeleted = isDeleted,
    createdAtMs = createdAt.toEpochMilli(),
    updatedAtMs = updatedAt.toEpochMilli()
)

fun ContainerEntity.toDomain(): Container = Container(
    id = id,
    name = name,
    amountMl = Milliliters(amountMl),
    isDefault = isDefault,
    sort = sort,
    symbol = symbol,
    isDeleted = isDeleted,
    createdAt = Instant.ofEpochMilli(createdAtMs),
    updatedAt = Instant.ofEpochMilli(updatedAtMs)
)

fun Container.toEntity(): ContainerEntity = ContainerEntity(
    id = id,
    name = name,
    amountMl = amountMl.value,
    isDefault = isDefault,
    sort = sort,
    symbol = symbol,
    isDeleted = isDeleted,
    createdAtMs = createdAt.toEpochMilli(),
    updatedAtMs = updatedAt.toEpochMilli()
)

fun ProfileEntity.toDomain(): UserProfile = UserProfile(
    preferredUnit = VolumeUnit.fromRaw(preferredUnit),
    bodyMassKg = bodyMassKg,
    activityLevel = ActivityLevel.entries.firstOrNull { it.name.equals(activityLevel, true) }
        ?: ActivityLevel.SEDENTARY,
    wakeTime = ClockTime(wakeHour, wakeMinute),
    sleepTime = ClockTime(sleepHour, sleepMinute),
    remindersEnabled = remindersEnabled,
    afterLastSipMinutes = afterLastSipMinutes,
    healthConnectEnabled = healthConnectEnabled,
    hapticsEnabled = hapticsEnabled,
    onboardingComplete = onboardingComplete,
    updatedAt = Instant.ofEpochMilli(updatedAtMs)
)

fun UserProfile.toEntity(): ProfileEntity = ProfileEntity(
    preferredUnit = preferredUnit.name.lowercase(),
    bodyMassKg = bodyMassKg,
    activityLevel = activityLevel.name.lowercase(),
    wakeHour = wakeTime.hour,
    wakeMinute = wakeTime.minute,
    sleepHour = sleepTime.hour,
    sleepMinute = sleepTime.minute,
    remindersEnabled = remindersEnabled,
    afterLastSipMinutes = afterLastSipMinutes,
    healthConnectEnabled = healthConnectEnabled,
    hapticsEnabled = hapticsEnabled,
    onboardingComplete = onboardingComplete,
    updatedAtMs = updatedAt.toEpochMilli()
)

fun GoalEntity.toDomain(): GoalSettings = GoalSettings(
    mode = GoalMode.entries.firstOrNull { it.name.equals(mode, true) } ?: GoalMode.MANUAL,
    manualGoalMl = Milliliters(manualGoalMl),
    updatedAt = Instant.ofEpochMilli(updatedAtMs)
)

fun GoalSettings.toEntity(): GoalEntity = GoalEntity(
    mode = mode.name.lowercase(),
    manualGoalMl = manualGoalMl.value,
    updatedAtMs = updatedAt.toEpochMilli()
)

fun ReminderEntity.toDomain(): ReminderRule = ReminderRule(
    enabled = enabled,
    start = ClockTime(startHour, startMinute),
    end = ClockTime(endHour, endMinute),
    intervalMinutes = intervalMinutes,
    afterLastSipMinutes = afterLastSipMinutes,
    updatedAt = Instant.ofEpochMilli(updatedAtMs)
)

fun ReminderRule.toEntity(): ReminderEntity = ReminderEntity(
    enabled = enabled,
    startHour = start.hour,
    startMinute = start.minute,
    endHour = end.hour,
    endMinute = end.minute,
    intervalMinutes = intervalMinutes,
    afterLastSipMinutes = afterLastSipMinutes,
    updatedAtMs = updatedAt.toEpochMilli()
)
