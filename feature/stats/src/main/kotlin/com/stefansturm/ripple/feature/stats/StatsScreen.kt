package com.stefansturm.ripple.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefansturm.ripple.core.designsystem.EmptyState
import com.stefansturm.ripple.core.designsystem.GlassCard
import com.stefansturm.ripple.core.designsystem.RippleColors
import com.stefansturm.ripple.core.designsystem.RippleMetrics
import com.stefansturm.ripple.core.domain.ChartPoint
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.StatsPeriod
import com.stefansturm.ripple.core.domain.StatsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

data class StatsUiState(val period: StatsPeriod = StatsPeriod.WEEK, val snapshot: StatsSnapshot? = null)

class StatsViewModel(private val repository: RippleRepository) : ViewModel() {
    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            period.flatMapLatest { repository.observeStats(it) }.collect { snapshot ->
                _state.update { it.copy(period = snapshot.period, snapshot = snapshot) }
            }
        }
    }

    fun select(period: StatsPeriod) { this.period.value = period }

    companion object {
        fun factory(repository: RippleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StatsViewModel(repository) as T
        }
    }
}

@Composable
fun StatsRoute(
    repository: RippleRepository,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory(repository))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsScreen(state, viewModel::select, modifier)
}

@Composable
private fun StatsScreen(state: StatsUiState, onPeriodSelected: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(RippleMetrics.lg),
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)
    ) {
        item { Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleLarge) }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(StatsPeriod.WEEK, StatsPeriod.MONTH, StatsPeriod.YEAR).forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onPeriodSelected(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        icon = {},
                        modifier = Modifier.weight(1f)
                    ) { Text(periodLabel(period)) }
                }
            }
        }
        state.snapshot?.let { snapshot ->
            item { SummaryRow(snapshot) }
            item { ChartCard(stringResource(R.string.stats_daily), snapshot.daily, showGoal = true) }
            item { ChartCard(stringResource(R.string.stats_daypart), snapshot.byDaypart) }
            item { ChartCard(stringResource(R.string.stats_container), snapshot.byContainer) }
            item { Highlights(snapshot) }
            if (snapshot.totalMl.value == 0) {
                item { EmptyState(stringResource(R.string.stats_no_data), stringResource(R.string.stats_no_data)) }
            }
        }
    }
}

@Composable
private fun SummaryRow(snapshot: StatsSnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md)) {
        SummaryCard(stringResource(R.string.stats_average), "${snapshot.averagePerDayMl.value} ml", Modifier.weight(1f))
        SummaryCard(stringResource(R.string.stats_hit_days), snapshot.hitDays.toString(), Modifier.weight(1f))
        SummaryCard(stringResource(R.string.stats_total), "${snapshot.totalMl.value} ml", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.md)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, points: List<ChartPoint>, showGoal: Boolean = false) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.lg), verticalArrangement = Arrangement.spacedBy(RippleMetrics.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (points.all { it.valueMl.value == 0 }) {
                Text(stringResource(R.string.stats_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SimpleBarChart(points = points, showGoal = showGoal)
            }
        }
    }
}

@Composable
private fun SimpleBarChart(points: List<ChartPoint>, showGoal: Boolean) {
    val max = maxOf(points.maxOfOrNull { it.valueMl.value } ?: 0, points.maxOfOrNull { it.goalMl?.value ?: 0 } ?: 0, 1)
    Canvas(
        Modifier.fillMaxWidth().height(RippleMetrics.chartHeight).semantics {
            contentDescription = points.joinToString { "${it.label}: ${it.valueMl.value} ml" }
        }
    ) {
        val gap = 8.dp.toPx()
        val barWidth = ((size.width - gap * (points.size + 1)) / points.size.coerceAtLeast(1)).coerceAtLeast(4.dp.toPx())
        points.forEachIndexed { index, point ->
            val x = gap + index * (barWidth + gap)
            val height = size.height * point.valueMl.value / max.toFloat()
            drawRoundRect(
                color = RippleColors.aqua,
                topLeft = Offset(x, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            val goal = point.goalMl
            if (showGoal && goal != null) {
                val goalY = size.height - size.height * goal.value / max.toFloat()
                drawLine(RippleColors.lagoon, Offset(x, goalY), Offset(x + barWidth, goalY), 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun Highlights(snapshot: StatsSnapshot) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.lg), verticalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
            Text(stringResource(R.string.stats_highlights), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.stats_best_day), style = MaterialTheme.typography.labelLarge)
            Text(snapshot.bestDay?.let { "${it.date}: ${it.consumedMl.value} ml" } ?: stringResource(R.string.stats_no_best_day))
            Text(stringResource(R.string.stats_current_run), style = MaterialTheme.typography.labelLarge)
            Text("${snapshot.currentHitRun} ${stringResource(R.string.stats_days)}")
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod): String = when (period) {
    StatsPeriod.WEEK -> stringResource(R.string.stats_week)
    StatsPeriod.MONTH -> stringResource(R.string.stats_month)
    StatsPeriod.YEAR -> stringResource(R.string.stats_year)
}
