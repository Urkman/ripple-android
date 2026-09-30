package com.stefansturm.ripple.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.domain.ActivityLevel
import com.stefansturm.ripple.core.domain.GoalMode
import com.stefansturm.ripple.core.domain.GoalSettings
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.UpdateGoal
import com.stefansturm.ripple.core.domain.UpdateProfile
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.VolumeUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefansturm.ripple.core.designsystem.GlassCard
import com.stefansturm.ripple.core.designsystem.RippleColors
import com.stefansturm.ripple.core.designsystem.RippleMetrics
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val unit: VolumeUnit = VolumeUnit.MILLILITERS,
    val goalMl: Int = 2000,
    val activity: ActivityLevel = ActivityLevel.SEDENTARY,
    val remindersEnabled: Boolean = true,
    val page: Int = 0
)

class OnboardingViewModel(private val repository: RippleRepository) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setPage(page: Int) { _state.value = _state.value.copy(page = page) }
    fun setUnit(unit: VolumeUnit) { _state.value = _state.value.copy(unit = unit) }
    fun setGoal(goal: Int) { _state.value = _state.value.copy(goalMl = goal) }
    fun setActivity(activity: ActivityLevel) { _state.value = _state.value.copy(activity = activity) }
    fun setReminders(enabled: Boolean) { _state.value = _state.value.copy(remindersEnabled = enabled) }

    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _state.value
            UpdateProfile(repository)(UserProfile(preferredUnit = state.unit, activityLevel = state.activity, remindersEnabled = state.remindersEnabled, onboardingComplete = true, updatedAt = Instant.now()))
            UpdateGoal(repository)(GoalSettings(mode = GoalMode.MANUAL, manualGoalMl = Milliliters(state.goalMl), updatedAt = Instant.now()))
            onFinished()
        }
    }

    companion object {
        fun factory(repository: RippleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(repository) as T
        }
    }
}

@Composable
fun OnboardingRoute(
    repository: RippleRepository,
    onFinished: () -> Unit,
    onRequestHealthConnect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(repository))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(state, viewModel, onFinished, onRequestHealthConnect, modifier)
}

@Composable
private fun OnboardingScreen(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onRequestHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLast = state.page == 5
    Surface(modifier = modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(RippleMetrics.xl),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RippleMetrics.md), modifier = Modifier.fillMaxWidth()) {
                Text("${state.page + 1} / 6", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                when (state.page) {
                    0 -> WelcomePage()
                    1 -> UnitPage(state, viewModel)
                    2 -> HealthPage(onRequestHealthConnect)
                    3 -> GoalPage(state, viewModel)
                    4 -> ContainersPage()
                    5 -> ReminderPage(state, viewModel)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md), verticalAlignment = Alignment.CenterVertically) {
                if (state.page > 0) {
                    OutlinedButton(onClick = { viewModel.setPage(state.page - 1) }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.onboarding_back)) }
                }
                Button(onClick = { if (isLast) viewModel.finish(onFinished) else viewModel.setPage(state.page + 1) }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) {
                    Text(stringResource(if (isLast) R.string.onboarding_finish else R.string.onboarding_next))
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnitPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    SetupPage(stringResource(R.string.onboarding_units_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
            FilterChip(selected = state.unit == VolumeUnit.MILLILITERS, onClick = { viewModel.setUnit(VolumeUnit.MILLILITERS) }, label = { Text("ml") })
            FilterChip(selected = state.unit == VolumeUnit.FLUID_OUNCES, onClick = { viewModel.setUnit(VolumeUnit.FLUID_OUNCES) }, label = { Text("fl oz") })
        }
    }
}

@Composable
private fun HealthPage(onRequestHealthConnect: () -> Unit) {
    SetupPage(stringResource(R.string.onboarding_health_title)) {
        Text(stringResource(R.string.onboarding_health_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRequestHealthConnect, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.onboarding_next)) }
    }
}

@Composable
private fun GoalPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    SetupPage(stringResource(R.string.onboarding_goal_title)) {
        Text(stringResource(R.string.onboarding_goal_value, state.goalMl), style = MaterialTheme.typography.titleLarge)
        Slider(value = state.goalMl.toFloat(), onValueChange = { viewModel.setGoal((it / 50).toInt() * 50) }, valueRange = 500f..5000f, steps = 89)
        Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
            listOf(ActivityLevel.SEDENTARY, ActivityLevel.MODERATE, ActivityLevel.HIGH).forEach { activity ->
                FilterChip(
                    selected = state.activity == activity,
                    onClick = { viewModel.setActivity(activity) },
                    label = {
                        Text(
                            stringResource(
                                when (activity) {
                                    ActivityLevel.SEDENTARY -> R.string.onboarding_activity_sedentary
                                    ActivityLevel.MODERATE -> R.string.onboarding_activity_moderate
                                    ActivityLevel.HIGH -> R.string.onboarding_activity_high
                                }
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ContainersPage() {
    SetupPage(stringResource(R.string.onboarding_containers_title)) {
        Text(stringResource(R.string.onboarding_containers_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassCard(modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_glass), modifier = Modifier.padding(RippleMetrics.lg)) }
        GlassCard(modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_cup), modifier = Modifier.padding(RippleMetrics.lg)) }
        GlassCard(modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_bottle), modifier = Modifier.padding(RippleMetrics.lg)) }
    }
}

@Composable
private fun ReminderPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    SetupPage(stringResource(R.string.onboarding_reminders_title)) {
        Text(stringResource(R.string.onboarding_reminders_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.onboarding_reminders_enable), modifier = Modifier.weight(1f))
            Switch(checked = state.remindersEnabled, onCheckedChange = viewModel::setReminders)
        }
    }
}

@Composable
private fun SetupPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = RippleMetrics.xxl), verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)) {
        Text(title, style = MaterialTheme.typography.displayMedium)
        content()
    }
}
