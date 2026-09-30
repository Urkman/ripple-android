package com.stefansturm.ripple.core.storage

import androidx.room.withTransaction
import com.stefansturm.ripple.core.domain.ActivityLevel
import com.stefansturm.ripple.core.domain.ChartPoint
import com.stefansturm.ripple.core.domain.ClockTime
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.DaySummary
import com.stefansturm.ripple.core.domain.ExportPayload
import com.stefansturm.ripple.core.domain.GoalCalculator
import com.stefansturm.ripple.core.domain.GoalSettings
import com.stefansturm.ripple.core.domain.HistorySnapshot
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.StatsPeriod
import com.stefansturm.ripple.core.domain.StatsSnapshot
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.VolumeUnit
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomRippleRepository(
    private val database: RippleDatabase
) : RippleRepository {
    private val dao = database.rippleDao()

    override fun observeProfile(): Flow<UserProfile> = dao.observeProfile().map { it?.toDomain() ?: UserProfile() }

    override fun observeGoal(): Flow<GoalSettings> = dao.observeGoal().map { it?.toDomain() ?: GoalSettings() }

    override fun observeReminder(): Flow<ReminderRule> = dao.observeReminder().map { it?.toDomain() ?: ReminderRule() }

    override fun observeContainers(): Flow<List<Container>> = dao.observeActiveContainers().map { rows -> rows.map { it.toDomain() } }

    override fun observeToday(zone: ZoneId, date: LocalDate): Flow<TodaySnapshot> {
        val (start, end) = dateBounds(date, zone)
        return combine(
            dao.observeIntakesBetween(start, end),
            observeContainers(),
            observeProfile(),
            observeGoal()
        ) { intakeRows, containers, profile, goalSettings ->
            val entries = intakeRows.map { it.toDomain() }.filterNot { it.isDeleted }
            val goal = GoalCalculator.calculate(profile, goalSettings)
            val consumed = Milliliters(entries.sumOf { it.amountMl.value })
            val remaining = Milliliters((goal.value - consumed.value).coerceAtLeast(0))
            TodaySnapshot(
                date = date,
                consumedMl = consumed,
                goalMl = goal,
                remainingMl = remaining,
                percent = if (goal.value == 0) 0 else (consumed.value * 100f / goal.value).roundToInt(),
                entries = entries,
                preferredUnit = profile.preferredUnit,
                defaultAddMl = containers.firstOrNull { it.isDefault }?.amountMl
                    ?: containers.firstOrNull()?.amountMl
                    ?: Milliliters(250),
                containers = containers
            )
        }
    }

    override fun observeMonth(month: YearMonth, zone: ZoneId): Flow<HistorySnapshot> {
        val startDate = month.atDay(1)
        val endDate = month.plusMonths(1).atDay(1)
        val (startMs, endMs) = dateBounds(startDate, zone).first to dateBounds(endDate, zone).first
        return combine(dao.observeIntakesBetween(startMs, endMs), observeProfile(), observeGoal()) { rows, profile, goalSettings ->
            val goal = GoalCalculator.calculate(profile, goalSettings)
            val grouped = rows.map { it.toDomain() }.filterNot { it.isDeleted }.groupBy { it.date.atZone(zone).toLocalDate() }
            HistorySnapshot(
                month = month,
                days = (1..month.lengthOfMonth()).map { day ->
                    val date = month.atDay(day)
                    val entries = grouped[date].orEmpty()
                    DaySummary(date, Milliliters(entries.sumOf { it.amountMl.value }), goal, entries.size)
                }
            )
        }
    }

    override fun observeDay(date: LocalDate, zone: ZoneId): Flow<DayDetailSnapshot> {
        val (start, end) = dateBounds(date, zone)
        return combine(dao.observeIntakesBetween(start, end), observeProfile(), observeGoal()) { rows, profile, goalSettings ->
            val entries = rows.map { it.toDomain() }.filterNot { it.isDeleted }
            val goal = GoalCalculator.calculate(profile, goalSettings)
            val summary = DaySummary(date, Milliliters(entries.sumOf { it.amountMl.value }), goal, entries.size)
            DayDetailSnapshot(date, summary, entries, profile.preferredUnit)
        }
    }

    override fun observeStats(period: StatsPeriod, zone: ZoneId): Flow<StatsSnapshot> {
        val today = LocalDate.now(zone)
        val start = when (period) {
            StatsPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            StatsPeriod.MONTH -> today.withDayOfMonth(1)
            StatsPeriod.YEAR -> today.withDayOfYear(1)
        }
        val end = today.plusDays(1)
        val (startMs, endMs) = dateBounds(start, zone).first to dateBounds(end, zone).first
        return combine(
            dao.observeIntakesBetween(startMs, endMs),
            observeContainers(),
            observeProfile(),
            observeGoal()
        ) { rows, containers, profile, goalSettings ->
            val goal = GoalCalculator.calculate(profile, goalSettings)
            val entries = rows.map { it.toDomain() }.filterNot { it.isDeleted }
            val groupedByDate = entries.groupBy { it.date.atZone(zone).toLocalDate() }
            val days = generateSequence(start) { current ->
                current.plusDays(1).takeIf { it.isBefore(end) }
            }.toList()
            val summaries = days.map { day ->
                val dayEntries = groupedByDate[day].orEmpty()
                DaySummary(day, Milliliters(dayEntries.sumOf { it.amountMl.value }), goal, dayEntries.size)
            }
            val total = Milliliters(entries.sumOf { it.amountMl.value })
            val labels = containers.associateBy { it.id }
            val byContainer = entries.groupBy { it.containerId }
                .map { (id, values) ->
                    ChartPoint(labels[id]?.name ?: "Other", Milliliters(values.sumOf { it.amountMl.value }))
                }
                .sortedByDescending { it.valueMl.value }
            val byDaypart = entries.groupBy { daypartLabel(it.date.atZone(zone).hour) }
                .map { (label, values) -> ChartPoint(label, Milliliters(values.sumOf { it.amountMl.value })) }
                .sortedBy { it.label }
            StatsSnapshot(
                period = period,
                start = start,
                endExclusive = end,
                goalMl = goal,
                averagePerDayMl = Milliliters(if (days.isEmpty()) 0 else total.value / days.size),
                hitDays = summaries.count { it.consumedMl >= it.goalMl },
                totalMl = total,
                daily = summaries.map { ChartPoint(it.date.toString(), it.consumedMl, it.goalMl) },
                byDaypart = byDaypart,
                byContainer = byContainer,
                bestDay = summaries.maxByOrNull { it.consumedMl.value },
                currentHitRun = currentHitRun(summaries, today)
            )
        }
    }

    override suspend fun logIntake(command: LogIntakeCommand): Intake = database.withTransaction {
        val now = Instant.now()
        val id = command.explicitId ?: UUID.randomUUID().toString()
        val existing = dao.findIntake(id)
        if (existing != null) return@withTransaction existing.toDomain()
        val intake = Intake(
            id = id,
            date = command.date,
            amountMl = command.amountMl,
            source = command.source,
            containerId = command.containerId,
            note = command.note,
            createdAt = now,
            updatedAt = now
        )
        dao.insertIntake(intake.toEntity())
        intake
    }

    override suspend fun undoLastIntake(): Intake? = database.withTransaction {
        val row = dao.findLatestActiveIntake() ?: return@withTransaction null
        val updated = row.copy(isDeleted = true, updatedAtMs = Instant.now().toEpochMilli())
        dao.updateIntake(updated)
        updated.toDomain()
    }

    override suspend fun editIntake(id: String, amountMl: Milliliters, date: Instant, note: String?): Intake = database.withTransaction {
        val row = dao.findIntake(id) ?: error("Intake not found")
        require(!row.isDeleted) { "Deleted intake cannot be edited" }
        val updated = row.copy(
            amountMl = amountMl.value,
            dateMs = date.toEpochMilli(),
            note = note,
            updatedAtMs = Instant.now().toEpochMilli()
        )
        dao.updateIntake(updated)
        updated.toDomain()
    }

    override suspend fun deleteIntake(id: String): Intake = database.withTransaction {
        val row = dao.findIntake(id) ?: error("Intake not found")
        val updated = row.copy(isDeleted = true, updatedAtMs = Instant.now().toEpochMilli())
        dao.updateIntake(updated)
        updated.toDomain()
    }

    override suspend fun restoreIntake(id: String): Intake = database.withTransaction {
        val row = dao.findIntake(id) ?: error("Intake not found")
        val updated = row.copy(isDeleted = false, updatedAtMs = Instant.now().toEpochMilli())
        dao.updateIntake(updated)
        updated.toDomain()
    }

    override suspend fun upsertContainer(container: Container): Container = database.withTransaction {
        val now = Instant.now().toEpochMilli()
        val existing = dao.findContainer(container.id)
        val saved = container.copy(
            createdAt = existing?.let { Instant.ofEpochMilli(it.createdAtMs) } ?: container.createdAt,
            updatedAt = Instant.ofEpochMilli(now),
            isDeleted = false
        )
        if (saved.isDefault) dao.clearContainerDefaults(now)
        dao.upsertContainer(saved.toEntity())
        if (!saved.isDefault && dao.firstActiveContainer()?.isDefault != true) {
            dao.firstActiveContainer()?.let { dao.setContainerDefault(it.id, now) }
        }
        saved
    }

    override suspend fun deleteContainer(id: String) = database.withTransaction {
        val existing = dao.findContainer(id) ?: return@withTransaction
        val now = Instant.now().toEpochMilli()
        dao.softDeleteContainer(id, now)
        if (existing.isDefault) dao.firstActiveContainer()?.let { dao.setContainerDefault(it.id, now) }
    }

    override suspend fun updateGoal(settings: GoalSettings): GoalSettings = database.withTransaction {
        val saved = settings.copy(updatedAt = Instant.now())
        dao.upsertGoal(saved.toEntity())
        saved
    }

    override suspend fun updateProfile(profile: UserProfile): UserProfile = database.withTransaction {
        val saved = profile.copy(updatedAt = Instant.now())
        dao.upsertProfile(saved.toEntity())
        saved
    }

    override suspend fun updateReminder(rule: ReminderRule): ReminderRule = database.withTransaction {
        val saved = rule.copy(updatedAt = Instant.now())
        dao.upsertReminder(saved.toEntity())
        saved
    }

    override suspend fun exportData(includeDeleted: Boolean): ExportPayload {
        val rows = dao.allIntakes()
        val filtered = rows.map { it.toDomain() }.filter { includeDeleted || !it.isDeleted }
        val now = Instant.now()
        val json = buildString {
            append("{\"version\":1,\"createdAt\":\"").append(now).append("\",\"intakes\":[")
            filtered.forEachIndexed { index, intake ->
                if (index > 0) append(',')
                append("{\"id\":\"").append(intake.id).append("\",\"date\":\"")
                    .append(intake.date).append("\",\"amountMl\":").append(intake.amountMl.value)
                    .append(",\"source\":\"").append(intake.source.name.lowercase())
                    .append("\",\"deleted\":").append(intake.isDeleted).append('}')
            }
            append("]}")
        }
        val csv = buildString {
            appendLine("id,date,amountMl,source,containerId,isDeleted")
            filtered.forEach { intake ->
                appendLine(listOf(intake.id, intake.date, intake.amountMl.value, intake.source.name.lowercase(), intake.containerId.orEmpty(), intake.isDeleted).joinToString(",") { value -> csvEscape(value.toString()) })
            }
        }
        return ExportPayload(json, csv, now)
    }

    suspend fun ensureSeeded() = database.withTransaction {
        val now = Instant.now()
        if (dao.findProfile() == null) dao.upsertProfile(UserProfile(updatedAt = now).toEntity())
        if (dao.findGoal() == null) dao.upsertGoal(GoalSettings(updatedAt = now).toEntity())
        if (dao.findReminder() == null) dao.upsertReminder(ReminderRule(updatedAt = now).toEntity())
        if (dao.allContainers().none { !it.isDeleted }) {
            dao.upsertContainer(seedContainer("Glass", 250, true, 0, "glass", now))
            dao.upsertContainer(seedContainer("Cup", 200, false, 1, "cup", now))
            dao.upsertContainer(seedContainer("Bottle", 500, false, 2, "bottle", now))
        }
    }

    private fun seedContainer(name: String, amount: Int, isDefault: Boolean, sort: Int, symbol: String, now: Instant) =
        Container(
            id = "seed-${name.lowercase()}",
            name = name,
            amountMl = Milliliters(amount),
            isDefault = isDefault,
            sort = sort,
            symbol = symbol,
            createdAt = now,
            updatedAt = now
        ).toEntity()

    private fun dateBounds(date: LocalDate, zone: ZoneId): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun daypartLabel(hour: Int): String = when (hour) {
        in 5..10 -> "Morning"
        in 11..13 -> "Midday"
        in 14..17 -> "Afternoon"
        else -> "Evening"
    }

    private fun currentHitRun(summaries: List<DaySummary>, today: LocalDate): Int {
        val byDate = summaries.associateBy { it.date }
        var cursor = today
        var run = 0
        while (byDate[cursor]?.let { it.consumedMl >= it.goalMl } == true) {
            run += 1
            cursor = cursor.minusDays(1)
        }
        return run
    }

    private fun csvEscape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
