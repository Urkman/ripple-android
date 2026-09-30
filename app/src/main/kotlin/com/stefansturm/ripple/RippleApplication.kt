package com.stefansturm.ripple

import android.app.Application
import android.content.Context
import com.stefansturm.ripple.core.health.HealthConnectGateway
import com.stefansturm.ripple.core.preferences.WidgetProjectionStore
import com.stefansturm.ripple.core.storage.RoomRippleRepository
import com.stefansturm.ripple.core.storage.RippleDatabase
import com.stefansturm.ripple.core.storage.createRippleDatabase
import com.stefansturm.ripple.system.notifications.ReminderScheduler
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.ExportPayload
import com.stefansturm.ripple.core.domain.GoalSettings
import com.stefansturm.ripple.core.domain.HistorySnapshot
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.StatsPeriod
import com.stefansturm.ripple.core.domain.StatsSnapshot
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.Container
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RippleApplication : Application() {
    lateinit var graph: RippleAppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = RippleAppGraph(this)
    }
}

class RippleAppGraph(context: Context) {
    val context: Context = context.applicationContext
    val database: RippleDatabase = createRippleDatabase(this.context)
    private val localRepository = RoomRippleRepository(database)
    val healthConnect = HealthConnectGateway(this.context)
    val widgetProjection = WidgetProjectionStore(this.context)
    private val reminderScheduler = ReminderScheduler(this.context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository: RippleRepository = ProjectingRippleRepository(localRepository, healthConnect, scope)

    init {
        scope.launch {
            localRepository.ensureSeeded()
            localRepository.observeToday().collect { widgetProjection.write(it) }
        }
        scope.launch {
            localRepository.observeReminder().collect { reminderScheduler.schedule(it) }
        }
    }
}

private class ProjectingRippleRepository(
    private val local: RippleRepository,
    private val health: HealthConnectGateway,
    private val scope: CoroutineScope
) : RippleRepository {
    override fun observeProfile(): Flow<UserProfile> = local.observeProfile()
    override fun observeGoal(): Flow<GoalSettings> = local.observeGoal()
    override fun observeReminder(): Flow<ReminderRule> = local.observeReminder()
    override fun observeContainers(): Flow<List<Container>> = local.observeContainers()
    override fun observeToday(zone: ZoneId, date: LocalDate): Flow<TodaySnapshot> = local.observeToday(zone, date)
    override fun observeMonth(month: YearMonth, zone: ZoneId): Flow<HistorySnapshot> = local.observeMonth(month, zone)
    override fun observeDay(date: LocalDate, zone: ZoneId): Flow<DayDetailSnapshot> = local.observeDay(date, zone)
    override fun observeStats(period: StatsPeriod, zone: ZoneId): Flow<StatsSnapshot> = local.observeStats(period, zone)

    override suspend fun logIntake(command: LogIntakeCommand): Intake {
        val intake = local.logIntake(command)
        scope.launch { health.project(intake) }
        return intake
    }

    override suspend fun undoLastIntake(): Intake? = local.undoLastIntake()
    override suspend fun editIntake(id: String, amountMl: Milliliters, date: Instant, note: String?): Intake = local.editIntake(id, amountMl, date, note)
    override suspend fun deleteIntake(id: String): Intake = local.deleteIntake(id)
    override suspend fun restoreIntake(id: String): Intake = local.restoreIntake(id)
    override suspend fun upsertContainer(container: Container): Container = local.upsertContainer(container)
    override suspend fun deleteContainer(id: String) = local.deleteContainer(id)
    override suspend fun updateGoal(settings: GoalSettings): GoalSettings = local.updateGoal(settings)
    override suspend fun updateProfile(profile: UserProfile): UserProfile = local.updateProfile(profile)
    override suspend fun updateReminder(rule: ReminderRule): ReminderRule = local.updateReminder(rule)
    override suspend fun exportData(includeDeleted: Boolean): ExportPayload = local.exportData(includeDeleted)
}
