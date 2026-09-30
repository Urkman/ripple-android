package com.stefansturm.ripple.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.DeleteIntake
import com.stefansturm.ripple.core.domain.EditIntake
import com.stefansturm.ripple.core.domain.HistorySnapshot
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RestoreIntake
import com.stefansturm.ripple.core.domain.RippleRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val snapshot: HistorySnapshot? = null
)

class HistoryViewModel(private val repository: RippleRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            month.flatMapLatest { repository.observeMonth(it) }.collect { snapshot ->
                _uiState.update { it.copy(month = snapshot.month, snapshot = snapshot) }
            }
        }
    }

    fun previousMonth() { month.update { it.minusMonths(1) } }
    fun nextMonth() { month.update { it.plusMonths(1) } }

    companion object {
        fun factory(repository: RippleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(repository) as T
        }
    }
}

data class DayDetailUiState(val snapshot: DayDetailSnapshot? = null, val message: String? = null)

class DayDetailViewModel(
    private val repository: RippleRepository,
    private val date: LocalDate
) : ViewModel() {
    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDay(date).collect { snapshot -> _uiState.update { it.copy(snapshot = snapshot) } }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            DeleteIntake(repository)(id)
            _uiState.update { it.copy(message = "deleted") }
        }
    }

    fun restore(id: String) {
        viewModelScope.launch {
            RestoreIntake(repository)(id)
            _uiState.update { it.copy(message = "restored") }
        }
    }

    fun edit(id: String, amount: Milliliters, date: Instant, note: String?) {
        viewModelScope.launch {
            EditIntake(repository)(id, amount, date, note)
            _uiState.update { it.copy(message = "saved") }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    companion object {
        fun factory(repository: RippleRepository, date: LocalDate): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DayDetailViewModel(repository, date) as T
        }
    }
}
