package com.stefansturm.ripple.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.stefansturm.ripple.core.designsystem.RippleColors
import com.stefansturm.ripple.core.domain.ChartPoint
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.DaySummary
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.domain.UnitConverter
import com.stefansturm.ripple.core.domain.VolumeUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun WearRippleApp(viewModel: WearViewModel = viewModel()) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val containers by viewModel.containers.collectAsStateWithLifecycle()
    val detailState by viewModel.detail.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var amountOpen by rememberSaveable { mutableStateOf(false) }
    var selectedDayRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDay = selectedDayRaw?.let(LocalDate::parse)
    val detail = detailState?.takeIf { it.date == selectedDay }

    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            delay(3_200)
            viewModel.clearFeedback()
        }
    }

    BackHandler(enabled = amountOpen || selectedDay != null) {
        if (amountOpen) amountOpen = false else selectedDayRaw = null
    }

    WearMaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WearMaterialTheme.colorScheme.background)
        ) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))
            when {
                amountOpen && today != null -> WearAmountDialog(
                    snapshot = today!!,
                    onDismiss = { amountOpen = false },
                    onConfirm = { amount, containerId ->
                        amountOpen = false
                        viewModel.log(amount, containerId)
                    }
                )
                selectedDay != null -> WearDayDetailPage(
                    detail = detail,
                    containers = containers,
                    onDelete = viewModel::delete
                )
                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> WearTodayPage(
                            snapshot = today,
                            pendingSyncCount = viewModel.pendingSyncCount(),
                            onOpenAmount = { amountOpen = true }
                        )
                        1 -> WearHistoryPage(
                            history = history,
                            onOpenDay = { date ->
                                selectedDayRaw = date.toString()
                                viewModel.selectDay(date)
                            }
                        )
                        else -> WearStatsPage(stats = stats)
                    }
                }
            }
            feedback?.let { current ->
                WearFeedbackOverlay(
                    feedback = current,
                    onUndo = viewModel::undo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun WearTodayPage(
    snapshot: TodaySnapshot?,
    pendingSyncCount: Int,
    onOpenAmount: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (snapshot == null) {
            WearLoadingPage(title = stringResource(R.string.wear_today))
            return@Box
        }
        val consumedText = amountText(snapshot.consumedMl, snapshot.preferredUnit)
        val goalText = amountText(snapshot.goalMl, snapshot.preferredUnit)
        val remainingText = amountText(snapshot.remainingMl, snapshot.preferredUnit)
        val levelDescription = stringResource(
            R.string.wear_consumed_accessibility,
            consumedText,
            goalText,
            remainingText,
            snapshot.percent
        )
        val level = if (snapshot.goalMl.value <= 0) 0f else {
            snapshot.consumedMl.value.toFloat() / snapshot.goalMl.value
        }.coerceIn(0f, 1f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(RippleColors.foamDark)
            val waterTop = size.height * (1f - level)
            drawRect(
                color = RippleColors.waterDeep,
                topLeft = androidx.compose.ui.geometry.Offset(0f, waterTop),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - waterTop)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 24.dp)
                    .semantics { contentDescription = levelDescription }
            ) {
                Text(stringResource(R.string.wear_today), color = Color.White)
                Text(text = consumedText, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    text = stringResource(R.string.wear_percent_goal, snapshot.percent, goalText),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.wear_left, remainingText),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val openAmountDescription = stringResource(R.string.wear_open_custom_amount)
                Button(
                    onClick = onOpenAmount,
                    modifier = Modifier
                        .size(64.dp)
                        .semantics { contentDescription = openAmountDescription }
                ) { Text("+") }
                if (pendingSyncCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.wear_sync_pending), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WearAmountDialog(
    snapshot: TodaySnapshot,
    onDismiss: () -> Unit,
    onConfirm: (Milliliters, String?) -> Unit
) {
    var amount by rememberSaveable { mutableIntStateOf(snapshot.defaultAddMl.value) }
    var selectedContainerId by rememberSaveable { mutableStateOf<String?>(snapshot.containers.firstOrNull()?.id) }
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val presets = snapshot.containers.take(3)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WearMaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))
            TransformingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent { event ->
                        val delta = if (event.verticalScrollPixels > 0) -10 else 10
                        amount = (amount + delta).coerceIn(50, 2_000)
                        selectedContainerId = null
                        true
                    },
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val currentAmountText = amountText(Milliliters(amount), snapshot.preferredUnit)
                    val currentAmountDescription = stringResource(R.string.wear_current_amount, currentAmountText)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.wear_custom_amount))
                        Text(
                            text = currentAmountText,
                            modifier = Modifier.semantics { contentDescription = currentAmountDescription }
                        )
                        Text(stringResource(R.string.wear_rotate_adjust))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            amount = (amount - 10).coerceAtLeast(50)
                            selectedContainerId = null
                        }) { Text("−10") }
                        Button(onClick = {
                            amount = (amount + 10).coerceAtMost(2_000)
                            selectedContainerId = null
                        }) { Text("+10") }
                    }
                }
                items(presets.size) { index ->
                    val container = presets[index]
                    val label = stringResource(
                        R.string.wear_preset_label,
                        container.name,
                        amountText(container.amountMl, snapshot.preferredUnit)
                    )
                    val containerDescription = stringResource(R.string.wear_preset_accessibility, label)
                    Button(
                        onClick = {
                            amount = container.amountMl.value
                            selectedContainerId = container.id
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = containerDescription }
                    ) {
                        Text(
                            text = if (selectedContainerId == container.id) {
                                stringResource(R.string.wear_selected_preset, label)
                            } else label
                        )
                    }
                }
                item {
                    Button(
                        onClick = { onConfirm(Milliliters(amount), selectedContainerId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = amount in 50..2_000 && amount % 10 == 0
                    ) { Text(stringResource(R.string.wear_confirm)) }
                }
            }
        }
    }
}

@Composable
private fun WearHistoryPage(
    history: com.stefansturm.ripple.core.domain.HistorySnapshot?,
    onOpenDay: (LocalDate) -> Unit
) {
    if (history == null) {
        WearLoadingPage(title = stringResource(R.string.wear_history))
        return
    }
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            WearPageHeading(
                title = stringResource(R.string.wear_history),
                subtitle = stringResource(R.string.wear_seven_days)
            )
        }
        items(history.days.size) { index ->
            val day = history.days[index]
            val dayDescription = stringResource(
                R.string.wear_day_accessibility,
                formatDate(day.date),
                amountText(day.consumedMl, VolumeUnit.MILLILITERS),
                amountText(day.goalMl, VolumeUnit.MILLILITERS),
                day.progressPercent()
            )
            Button(
                onClick = { onOpenDay(day.date) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = dayDescription }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDate(day.date))
                    Text(
                        stringResource(
                            R.string.wear_day_progress,
                            amountText(day.consumedMl, VolumeUnit.MILLILITERS),
                            day.progressPercent()
                        )
                    )
                    Text(
                        if (day.consumedMl >= day.goalMl) stringResource(R.string.wear_goal_reached)
                        else stringResource(
                            R.string.wear_goal,
                            amountText(day.goalMl, VolumeUnit.MILLILITERS)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WearDayDetailPage(
    detail: DayDetailSnapshot?,
    containers: List<Container>,
    onDelete: (com.stefansturm.ripple.core.domain.Intake) -> Unit
) {
    if (detail == null) {
        WearLoadingPage(title = stringResource(R.string.wear_history))
        return
    }
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            WearPageHeading(
                title = formatDate(detail.date),
                subtitle = stringResource(
                    R.string.wear_day_detail_summary,
                    amountText(detail.summary.consumedMl, detail.preferredUnit),
                    amountText(detail.summary.goalMl, detail.preferredUnit)
                )
            )
        }
        if (detail.entries.isEmpty()) {
            item {
                Text(stringResource(R.string.wear_no_entries), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(detail.entries.size) { index ->
                val intake = detail.entries[index]
                val containerName = containers.firstOrNull { it.id == intake.containerId }?.name
                    ?: stringResource(R.string.wear_water)
                val sourceName = sourceLabel(intake.source)
                val entryDescription = stringResource(
                    R.string.wear_entry_accessibility,
                    formatTime(intake.date),
                    amountText(intake.amountMl, detail.preferredUnit),
                    containerName,
                    sourceName
                )
                Card(
                    onClick = { onDelete(intake) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = entryDescription }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTime(intake.date))
                        Text(amountText(intake.amountMl, detail.preferredUnit))
                        Text(containerName)
                        Text(sourceName)
                        Text(stringResource(R.string.wear_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun WearStatsPage(stats: com.stefansturm.ripple.core.domain.StatsSnapshot?) {
    if (stats == null) {
        WearLoadingPage(title = stringResource(R.string.wear_stats))
        return
    }
    val weekText = stringResource(
        R.string.wear_date_range,
        formatDate(stats.start),
        formatDate(stats.endExclusive.minusDays(1))
    )
    val chartDescription = localizedChartDescription(stats.daily)
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            WearPageHeading(
                title = stringResource(R.string.wear_stats),
                subtitle = stringResource(R.string.wear_week_context, stringResource(R.string.wear_current_iso_week), weekText)
            )
        }
        item {
            SummaryRow(
                label = stringResource(R.string.wear_average_day),
                value = stringResource(R.string.wear_amount_value, stats.averagePerDayMl.value, VolumeUnit.MILLILITERS.symbol)
            )
        }
        item { SummaryRow(label = stringResource(R.string.wear_goal_hits), value = stats.hitDays.toString()) }
        item {
            SummaryRow(
                label = stringResource(R.string.wear_total),
                value = stringResource(R.string.wear_amount_value, stats.totalMl.value, VolumeUnit.MILLILITERS.symbol)
            )
        }
        item { WearChart(points = stats.daily, description = chartDescription) }
        item {
            Text(text = chartDescription, modifier = Modifier.semantics { contentDescription = chartDescription })
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(RippleColors.elevatedDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(value)
    }
}

@Composable
private fun WearChart(points: List<ChartPoint>, description: String) {
    val max = (points.maxOfOrNull { it.valueMl.value } ?: 0).coerceAtLeast(1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RippleColors.elevatedDark)
            .semantics { contentDescription = description }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val gap = 4.dp.toPx()
            val barWidth = ((size.width - gap * (points.size - 1)) / points.size.coerceAtLeast(1)).coerceAtLeast(2f)
            points.forEachIndexed { index, point ->
                val barHeight = size.height * point.valueMl.value / max.toFloat()
                drawRoundRect(
                    color = RippleColors.aqua,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        index * (barWidth + gap),
                        size.height - barHeight
                    ),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun WearFeedbackOverlay(
    feedback: WearFeedback,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RippleColors.elevatedDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(feedback.message)
        if (feedback.undoAction != null) {
            Button(onClick = onUndo) { Text(stringResource(R.string.wear_undo)) }
        }
    }
}

@Composable
private fun WearLoadingPage(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title)
        Text(stringResource(R.string.wear_loading))
    }
}

@Composable
private fun WearPageHeading(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title)
        Text(subtitle, textAlign = TextAlign.Center)
    }
}

private fun amountText(amount: Milliliters, unit: VolumeUnit): String =
    "${UnitConverter.toDisplay(amount, unit)} ${unit.symbol}"

private fun formatDate(date: LocalDate): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(date)

private fun formatTime(instant: java.time.Instant): String = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(Locale.getDefault())
    .format(instant.atZone(java.time.ZoneId.systemDefault()))

private fun DaySummary.progressPercent(): Int = (cappedProgress * 100).roundToInt()

@Composable
private fun sourceLabel(source: IntakeSource): String = stringResource(
    when (source) {
        IntakeSource.APP -> R.string.wear_source_app
        IntakeSource.WIDGET -> R.string.wear_source_widget
        IntakeSource.INTENT -> R.string.wear_source_intent
        IntakeSource.WATCH -> R.string.wear_source_watch
        IntakeSource.CONTROL -> R.string.wear_source_control
        IntakeSource.NOTIFICATION -> R.string.wear_source_notification
        IntakeSource.HEALTH -> R.string.wear_source_health
    }
)

@Composable
private fun localizedChartDescription(points: List<ChartPoint>): String {
    if (points.isEmpty()) return stringResource(R.string.wear_no_chart_data)
    val values = points.joinToString(", ") { "${it.label}: ${it.valueMl.value} ml" }
    return stringResource(R.string.wear_daily_totals, values)
}
