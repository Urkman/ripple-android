package com.stefansturm.ripple.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.domain.ActivityLevel
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.DeleteContainer
import com.stefansturm.ripple.core.domain.GoalMode
import com.stefansturm.ripple.core.domain.GoalSettings
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.StatsPeriod
import com.stefansturm.ripple.core.domain.UpdateGoal
import com.stefansturm.ripple.core.domain.UpdateProfile
import com.stefansturm.ripple.core.domain.UpsertContainer
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.VolumeUnit
import com.stefansturm.ripple.core.domain.RescheduleReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val goal: GoalSettings = GoalSettings(),
    val containers: List<Container> = emptyList(),
    val reminder: ReminderRule = ReminderRule(),
    val message: String? = null
)

class SettingsViewModel(private val repository: RippleRepository) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        repository.observeProfile(),
        repository.observeGoal(),
        repository.observeContainers(),
        repository.observeReminder()
    ) { profile, goal, containers, reminder -> SettingsUiState(profile, goal, containers, reminder) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUnit(unit: VolumeUnit) = launch { UpdateProfile(repository)(state.value.profile.copy(preferredUnit = unit)) }
    fun setActivity(level: ActivityLevel) = launch { UpdateProfile(repository)(state.value.profile.copy(activityLevel = level)) }
    fun setGoalMode(mode: GoalMode) = launch { UpdateGoal(repository)(state.value.goal.copy(mode = mode)) }
    fun setManualGoal(goalMl: Int) = launch { UpdateGoal(repository)(state.value.goal.copy(mode = GoalMode.MANUAL, manualGoalMl = com.stefansturm.ripple.core.domain.Milliliters(goalMl))) }
    fun saveContainer(container: Container) = launch { UpsertContainer(repository)(container) }
    fun deleteContainer(id: String) = launch { DeleteContainer(repository)(id) }
    fun saveReminder(rule: ReminderRule) = launch { RescheduleReminders(repository)(rule) }
    fun clearMessage() { _message.update { null } }

    private val _message = MutableStateFlow<String?>(null)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            _message.update { "saved" }
        }
    }

    companion object {
        fun factory(repository: RippleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
        }
    }
}
