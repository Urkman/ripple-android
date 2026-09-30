package com.stefansturm.ripple.core.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

@JvmInline
value class Milliliters(val value: Int) {
    operator fun plus(other: Milliliters) = Milliliters(value + other.value)
    operator fun minus(other: Milliliters) = Milliliters(value - other.value)
    operator fun compareTo(other: Milliliters): Int = value.compareTo(other.value)
}

enum class VolumeUnit(val symbol: String) {
    MILLILITERS("ml"),
    FLUID_OUNCES("fl oz");

    companion object {
        fun fromRaw(raw: String?): VolumeUnit = entries.firstOrNull { it.name.equals(raw, true) }
            ?: MILLILITERS
    }
}

enum class Beverage { WATER }

enum class IntakeSource {
    APP,
    WIDGET,
    INTENT,
    WATCH,
    CONTROL,
    NOTIFICATION,
    HEALTH;

    companion object {
        fun fromRaw(raw: String?): IntakeSource = entries.firstOrNull { it.name.equals(raw, true) }
            ?: APP
    }
}

enum class GoalMode { MANUAL, CALCULATED }

enum class ActivityLevel(val dailyBonusMl: Int) {
    SEDENTARY(0),
    MODERATE(350),
    HIGH(700)
}

enum class StatsPeriod { WEEK, MONTH, YEAR }

enum class PermissionStatus {
    UNAVAILABLE,
    NOT_DETERMINED,
    DENIED,
    PARTIAL,
    GRANTED,
    FAILED
}

data class ClockTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }

    fun totalMinutes(): Int = hour * 60 + minute
}

data class Intake(
    val id: String,
    val date: Instant,
    val amountMl: Milliliters,
    val beverage: Beverage = Beverage.WATER,
    val source: IntakeSource,
    val containerId: String? = null,
    val note: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Container(
    val id: String,
    val name: String,
    val amountMl: Milliliters,
    val isDefault: Boolean,
    val sort: Int,
    val symbol: String = "glass",
    val isDeleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UserProfile(
    val preferredUnit: VolumeUnit = VolumeUnit.MILLILITERS,
    val bodyMassKg: Int? = null,
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val wakeTime: ClockTime = ClockTime(7, 0),
    val sleepTime: ClockTime = ClockTime(22, 0),
    val remindersEnabled: Boolean = true,
    val afterLastSipMinutes: Int = 120,
    val healthConnectEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val updatedAt: Instant = Instant.EPOCH
)

data class GoalSettings(
    val mode: GoalMode = GoalMode.MANUAL,
    val manualGoalMl: Milliliters = Milliliters(2000),
    val updatedAt: Instant = Instant.EPOCH
)

data class ReminderRule(
    val enabled: Boolean = true,
    val start: ClockTime = ClockTime(7, 0),
    val end: ClockTime = ClockTime(22, 0),
    val intervalMinutes: Int = 120,
    val afterLastSipMinutes: Int = 120,
    val updatedAt: Instant = Instant.EPOCH
)

data class DaySummary(
    val date: LocalDate,
    val consumedMl: Milliliters,
    val goalMl: Milliliters,
    val entryCount: Int
) {
    val progress: Float
        get() = if (goalMl.value <= 0) 0f else consumedMl.value.toFloat() / goalMl.value
    val cappedProgress: Float
        get() = progress.coerceIn(0f, 1f)
}

data class TodaySnapshot(
    val date: LocalDate,
    val consumedMl: Milliliters,
    val goalMl: Milliliters,
    val remainingMl: Milliliters,
    val percent: Int,
    val entries: List<Intake>,
    val preferredUnit: VolumeUnit,
    val defaultAddMl: Milliliters,
    val containers: List<Container>,
    val pacingLabel: String? = null
) {
    val isOverGoal: Boolean get() = consumedMl > goalMl
    val cappedPercent: Int get() = percent.coerceIn(0, 100)
}

data class HistorySnapshot(
    val month: YearMonth,
    val days: List<DaySummary>
)

data class DayDetailSnapshot(
    val date: LocalDate,
    val summary: DaySummary,
    val entries: List<Intake>,
    val preferredUnit: VolumeUnit
)

data class ChartPoint(val label: String, val valueMl: Milliliters, val goalMl: Milliliters? = null)

data class StatsSnapshot(
    val period: StatsPeriod,
    val start: LocalDate,
    val endExclusive: LocalDate,
    val goalMl: Milliliters,
    val averagePerDayMl: Milliliters,
    val hitDays: Int,
    val totalMl: Milliliters,
    val daily: List<ChartPoint>,
    val byDaypart: List<ChartPoint>,
    val byContainer: List<ChartPoint>,
    val bestDay: DaySummary?,
    val currentHitRun: Int
)

data class LogIntakeCommand(
    val amountMl: Milliliters,
    val source: IntakeSource,
    val date: Instant = Instant.now(),
    val containerId: String? = null,
    val note: String? = null,
    val explicitId: String? = null
)

data class ExportPayload(
    val json: String,
    val csv: String,
    val createdAt: Instant
)

object RippleValidation {
    const val MIN_UI_AMOUNT_ML = 50
    const val MAX_UI_AMOUNT_ML = 2000
    const val UI_AMOUNT_STEP_ML = 10
    const val MAX_CONTAINER_NAME_LENGTH = 40

    fun validatePositiveAmount(amount: Milliliters) {
        require(amount.value > 0) { "Amount must be positive" }
    }

    fun validateUiAmount(amount: Milliliters) {
        require(amount.value in MIN_UI_AMOUNT_ML..MAX_UI_AMOUNT_ML) {
            "Amount must be between $MIN_UI_AMOUNT_ML and $MAX_UI_AMOUNT_ML ml"
        }
        require(amount.value % UI_AMOUNT_STEP_ML == 0) {
            "Amount must use $UI_AMOUNT_STEP_ML ml steps"
        }
    }

    fun validateContainerName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Container name must not be empty" }
        require(trimmed.length <= MAX_CONTAINER_NAME_LENGTH) {
            "Container name must be at most $MAX_CONTAINER_NAME_LENGTH characters"
        }
        return trimmed
    }

    fun validateReminder(rule: ReminderRule) {
        require(rule.intervalMinutes > 0) { "Reminder interval must be positive" }
        require(rule.afterLastSipMinutes > 0) { "After-last-sip interval must be positive" }
        require(rule.start.totalMinutes() < rule.end.totalMinutes()) {
            "Reminder start must be before reminder end"
        }
    }
}

object UnitConverter {
    private const val ML_PER_FLUID_OUNCE = 29.5735

    fun toDisplay(amountMl: Milliliters, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.MILLILITERS -> amountMl.value
        VolumeUnit.FLUID_OUNCES -> kotlin.math.round(amountMl.value / ML_PER_FLUID_OUNCE).toInt()
    }

    fun toMl(displayAmount: Int, unit: VolumeUnit): Milliliters = when (unit) {
        VolumeUnit.MILLILITERS -> Milliliters(displayAmount)
        VolumeUnit.FLUID_OUNCES -> Milliliters(kotlin.math.round(displayAmount * ML_PER_FLUID_OUNCE).toInt())
    }
}

object GoalCalculator {
    fun calculate(profile: UserProfile, goal: GoalSettings, workoutBonusMl: Int = 0): Milliliters {
        if (goal.mode == GoalMode.MANUAL && goal.manualGoalMl.value > 0) {
            return goal.manualGoalMl
        }
        val massBase = profile.bodyMassKg?.let { it * 33 } ?: 2000
        val roundedMassBase = ((massBase + 25) / 50) * 50
        val calculated = roundedMassBase + profile.activityLevel.dailyBonusMl + workoutBonusMl
        return Milliliters(calculated.coerceAtLeast(250))
    }
}

interface RippleRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeGoal(): Flow<GoalSettings>
    fun observeReminder(): Flow<ReminderRule>
    fun observeContainers(): Flow<List<Container>>
    fun observeToday(zone: ZoneId = ZoneId.systemDefault(), date: LocalDate = LocalDate.now(zone)): Flow<TodaySnapshot>
    fun observeMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Flow<HistorySnapshot>
    fun observeDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<DayDetailSnapshot>
    fun observeStats(period: StatsPeriod, zone: ZoneId = ZoneId.systemDefault()): Flow<StatsSnapshot>

    suspend fun logIntake(command: LogIntakeCommand): Intake
    suspend fun undoLastIntake(): Intake?
    suspend fun editIntake(id: String, amountMl: Milliliters, date: Instant, note: String?): Intake
    suspend fun deleteIntake(id: String): Intake
    suspend fun restoreIntake(id: String): Intake
    suspend fun upsertContainer(container: Container): Container
    suspend fun deleteContainer(id: String)
    suspend fun updateGoal(settings: GoalSettings): GoalSettings
    suspend fun updateProfile(profile: UserProfile): UserProfile
    suspend fun updateReminder(rule: ReminderRule): ReminderRule
    suspend fun exportData(includeDeleted: Boolean = true): ExportPayload
}

class LogIntake(private val repository: RippleRepository) {
    suspend operator fun invoke(command: LogIntakeCommand): Result<Intake> = runCatching {
        RippleValidation.validatePositiveAmount(command.amountMl)
        repository.logIntake(command)
    }
}

class UndoLastIntake(private val repository: RippleRepository) {
    suspend operator fun invoke(): Result<Intake?> = runCatching { repository.undoLastIntake() }
}

class EditIntake(private val repository: RippleRepository) {
    suspend operator fun invoke(id: String, amountMl: Milliliters, date: Instant, note: String?): Result<Intake> = runCatching {
        RippleValidation.validatePositiveAmount(amountMl)
        repository.editIntake(id, amountMl, date, note)
    }
}

class DeleteIntake(private val repository: RippleRepository) {
    suspend operator fun invoke(id: String): Result<Intake> = runCatching { repository.deleteIntake(id) }
}

class RestoreIntake(private val repository: RippleRepository) {
    suspend operator fun invoke(id: String): Result<Intake> = runCatching { repository.restoreIntake(id) }
}

class UpsertContainer(private val repository: RippleRepository) {
    suspend operator fun invoke(container: Container): Result<Container> = runCatching {
        val normalized = container.copy(name = RippleValidation.validateContainerName(container.name))
        RippleValidation.validateUiAmount(normalized.amountMl)
        repository.upsertContainer(normalized)
    }
}

class DeleteContainer(private val repository: RippleRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = runCatching { repository.deleteContainer(id) }
}

class UpdateGoal(private val repository: RippleRepository) {
    suspend operator fun invoke(settings: GoalSettings): Result<GoalSettings> = runCatching {
        require(settings.manualGoalMl.value > 0) { "Goal must be positive" }
        repository.updateGoal(settings)
    }
}

class UpdateProfile(private val repository: RippleRepository) {
    suspend operator fun invoke(profile: UserProfile): Result<UserProfile> = runCatching {
        require(profile.afterLastSipMinutes > 0) { "After-last-sip interval must be positive" }
        repository.updateProfile(profile)
    }
}

class RescheduleReminders(private val repository: RippleRepository) {
    suspend operator fun invoke(rule: ReminderRule): Result<ReminderRule> = runCatching {
        RippleValidation.validateReminder(rule)
        repository.updateReminder(rule)
    }
}

class ExportData(private val repository: RippleRepository) {
    suspend operator fun invoke(includeDeleted: Boolean = true): Result<ExportPayload> = runCatching {
        repository.exportData(includeDeleted)
    }
}
