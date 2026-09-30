package com.stefansturm.ripple.wear

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.HistorySnapshot
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.LogIntake
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RestoreIntake
import com.stefansturm.ripple.core.domain.StatsPeriod
import com.stefansturm.ripple.core.domain.StatsSnapshot
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.storage.RoomRippleRepository
import com.stefansturm.ripple.core.storage.createRippleDatabase
import com.stefansturm.ripple.core.wear.sync.WearMutationFactory
import com.stefansturm.ripple.core.wear.sync.WearMutationOperation
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class WearUndoAction { UNDO_LATEST, RESTORE_DELETED }

data class WearFeedback(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val undoAction: WearUndoAction? = null,
    val intakeId: String? = null
)

class WearViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val database = createRippleDatabase(application)
    private val repository = RoomRippleRepository(database)
    private val outbox = WearOutboxStore(application)
    private val syncClient = WearSyncClient(application, outbox)
    private val originDeviceId = application.getSharedPreferences(
        "ripple_wear_identity",
        Context.MODE_PRIVATE
    ).let { preferences ->
        preferences.getString("device_id", null) ?: UUID.randomUUID().toString().also { generated ->
            preferences.edit { putString("device_id", generated) }
        }
    }
    private val anchorDate = LocalDate.now()

    private val _today = MutableStateFlow<TodaySnapshot?>(null)
    val today: StateFlow<TodaySnapshot?> = _today.asStateFlow()

    private val recentDayFlows: List<Flow<DayDetailSnapshot>> = (0..6).map { offset ->
        repository.observeDay(anchorDate.minusDays(offset.toLong()))
    }
    val history: StateFlow<HistorySnapshot?> = combine(recentDayFlows) { details ->
        HistorySnapshot(YearMonth.from(anchorDate), details.map { it.summary })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<StatsSnapshot?> = repository.observeStats(StatsPeriod.WEEK)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val containers: StateFlow<List<Container>> = repository.observeContainers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _detail = MutableStateFlow<DayDetailSnapshot?>(null)
    val detail: StateFlow<DayDetailSnapshot?> = _detail.asStateFlow()

    private val _feedback = MutableStateFlow<WearFeedback?>(null)
    val feedback: StateFlow<WearFeedback?> = _feedback.asStateFlow()

    private var detailJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.observeToday().collect { _today.value = it }
        }
    }

    fun selectDay(date: LocalDate) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            repository.observeDay(date).collect { _detail.value = it }
        }
    }

    fun log(amountMl: Milliliters, containerId: String?) {
        viewModelScope.launch {
            LogIntake(repository)(
                LogIntakeCommand(
                    amountMl = amountMl,
                    source = IntakeSource.WATCH,
                    containerId = containerId
                )
            ).onSuccess { intake ->
                syncClient.enqueueAndSend(WearMutationFactory.forIntake(intake, originDeviceId))
                _feedback.value = WearFeedback(
                    message = app.getString(
                        R.string.wear_amount_added,
                        app.getString(R.string.wear_amount_value, amountMl.value, "ml")
                    ),
                    undoAction = WearUndoAction.UNDO_LATEST
                )
            }.onFailure { error ->
                _feedback.value = WearFeedback(message = error.message ?: app.getString(R.string.wear_log_error))
            }
        }
    }

    fun delete(intake: Intake) {
        viewModelScope.launch {
            runCatching { repository.deleteIntake(intake.id) }
                .onSuccess { deleted ->
                    syncClient.enqueueAndSend(
                        WearMutationFactory.forIntake(deleted, originDeviceId, WearMutationOperation.DELETE)
                    )
                    _feedback.value = WearFeedback(
                        message = app.getString(
                            R.string.wear_amount_deleted,
                            app.getString(R.string.wear_amount_value, deleted.amountMl.value, "ml")
                        ),
                        undoAction = WearUndoAction.RESTORE_DELETED,
                        intakeId = deleted.id
                    )
                }
                .onFailure { error -> _feedback.value = WearFeedback(message = error.message ?: app.getString(R.string.wear_delete_error)) }
        }
    }

    fun undo() {
        val current = _feedback.value ?: return
        viewModelScope.launch {
            when (current.undoAction) {
                WearUndoAction.UNDO_LATEST ->
                    runCatching { repository.undoLastIntake() }.onSuccess { restored ->
                        restored?.let { syncClient.enqueueAndSend(WearMutationFactory.forIntake(it, originDeviceId)) }
                    }
                WearUndoAction.RESTORE_DELETED ->
                    current.intakeId?.let { id ->
                        runCatching { repository.restoreIntake(id) }.onSuccess {
                            syncClient.enqueueAndSend(
                                WearMutationFactory.forIntake(it, originDeviceId, WearMutationOperation.RESTORE)
                            )
                        }
                    }
                null -> Unit
            }
            _feedback.value = null
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun pendingSyncCount(): Int = outbox.pendingCount()

    override fun onCleared() {
        detailJob?.cancel()
        database.close()
        super.onCleared()
    }
}
