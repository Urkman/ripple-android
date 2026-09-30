package com.stefansturm.ripple.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.LogIntake
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.domain.UndoLastIntake
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodayVisualEventKind { ADD, UNDO }

data class TodayVisualEvent(
    val sequence: Long,
    val kind: TodayVisualEventKind,
    val amountMl: Milliliters,
    val targetConsumedMl: Milliliters,
    val targetGoalMl: Milliliters
)

data class TodayUiState(
    val snapshot: TodaySnapshot? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val successAmountMl: Milliliters? = null,
    val canUndo: Boolean = false,
    val visualEvent: TodayVisualEvent? = null
)

class TodayViewModel(private val repository: RippleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeToday().collect { snapshot ->
                _uiState.update { it.copy(snapshot = snapshot) }
            }
        }
    }

    fun log(amountMl: Milliliters, containerId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, successAmountMl = null) }
            LogIntake(repository)(
                LogIntakeCommand(
                    amountMl = amountMl,
                    source = IntakeSource.APP,
                    containerId = containerId
                )
            ).onSuccess { intake ->
                val target = runCatching { repository.observeToday().first() }.getOrNull()
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        message = null,
                        successAmountMl = intake.amountMl,
                        canUndo = true,
                        visualEvent = target?.let { targetSnapshot ->
                            TodayVisualEvent(
                                sequence = (state.visualEvent?.sequence ?: 0L) + 1L,
                                kind = TodayVisualEventKind.ADD,
                                amountMl = intake.amountMl,
                                targetConsumedMl = targetSnapshot.consumedMl,
                                targetGoalMl = targetSnapshot.goalMl
                            )
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, message = error.message ?: "Could not save", successAmountMl = null, canUndo = false) }
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            UndoLastIntake(repository)().onSuccess { undone ->
                val target = runCatching { repository.observeToday().first() }.getOrNull()
                _uiState.update { state ->
                    state.copy(
                        message = null,
                        successAmountMl = null,
                        canUndo = false,
                        visualEvent = if (undone != null && target != null) {
                            TodayVisualEvent(
                                sequence = (state.visualEvent?.sequence ?: 0L) + 1L,
                                kind = TodayVisualEventKind.UNDO,
                                amountMl = undone.amountMl,
                                targetConsumedMl = target.consumedMl,
                                targetGoalMl = target.goalMl
                            )
                        } else {
                            state.visualEvent
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "Could not undo", successAmountMl = null, canUndo = false) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, successAmountMl = null, canUndo = false) }
    }

    companion object {
        fun factory(repository: RippleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TodayViewModel(repository) as T
        }
    }
}
