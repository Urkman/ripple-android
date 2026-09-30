package com.stefansturm.ripple.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefansturm.ripple.core.designsystem.DayRing
import com.stefansturm.ripple.core.designsystem.EmptyState
import com.stefansturm.ripple.core.designsystem.GlassCard
import com.stefansturm.ripple.core.designsystem.GlassCardRow
import com.stefansturm.ripple.core.designsystem.LogButton
import com.stefansturm.ripple.core.designsystem.RippleMetrics
import com.stefansturm.ripple.core.domain.DayDetailSnapshot
import com.stefansturm.ripple.core.domain.DaySummary
import com.stefansturm.ripple.core.domain.HistorySnapshot
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.UnitConverter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.res.stringResource

@Composable
fun HistoryRoute(
    repository: RippleRepository,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repository))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onPrevious = viewModel::previousMonth,
        onNext = viewModel::nextMonth,
        onOpenDay = onOpenDay,
        modifier = modifier
    )
}

@Composable
private fun HistoryScreen(
    state: HistoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = state.snapshot
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(RippleMetrics.lg),
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)
    ) {
        item {
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.history_previous))
                }
                Text(
                    state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.history_next))
                }
            }
        }
        snapshot?.let { history ->
            item { HistoryCalendar(history, onOpenDay) }
        }
    }
}

@Composable
private fun HistoryCalendar(snapshot: HistorySnapshot, onOpenDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val firstDayOffset = (snapshot.month.atDay(1).dayOfWeek.value + 6) % 7
    val cells = buildList<Any> {
        repeat(firstDayOffset) { add(Unit) }
        addAll(snapshot.days)
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.sm), verticalArrangement = Arrangement.spacedBy(RippleMetrics.xs)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                stringResource(R.string.history_weekdays).split('|').forEach { label ->
                    Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                }
            }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        if (cell is DaySummary) {
                            val enabled = !cell.date.isAfter(today)
                            DayRing(
                                summary = cell,
                                modifier = Modifier.weight(1f),
                                today = cell.date == today,
                                enabled = enabled,
                                onClick = if (enabled) ({ onOpenDay(cell.date) }) else null
                            )
                        } else {
                            Spacer(Modifier.weight(1f).size(58.dp))
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f).size(58.dp)) }
                }
            }
        }
    }
}

@Composable
fun DayDetailRoute(
    repository: RippleRepository,
    date: LocalDate,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = viewModel(factory = DayDetailViewModel.factory(repository, date))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        DayDetailScreen(
            snapshot = state.snapshot,
            padding = padding,
            onBack = onBack,
            onAdd = onAdd,
            onDelete = viewModel::delete,
            onRestore = viewModel::restore,
            onEdit = { editing = it.id }
        )
    }
    val intake = state.snapshot?.entries?.firstOrNull { it.id == editing }
    if (intake != null) {
        EditIntakeSheet(
            intake = intake,
            unit = state.snapshot?.preferredUnit ?: com.stefansturm.ripple.core.domain.VolumeUnit.MILLILITERS,
            onDismiss = { editing = null },
            onSave = { amount, note ->
                viewModel.edit(intake.id, amount, intake.date, note)
                editing = null
            }
        )
    }
}

@Composable
private fun DayDetailScreen(
    snapshot: DayDetailSnapshot?,
    padding: PaddingValues,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onEdit: (Intake) -> Unit
) {
    if (snapshot == null) return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = RippleMetrics.lg,
            top = padding.calculateTopPadding() + RippleMetrics.sm,
            end = RippleMetrics.lg,
            bottom = padding.calculateBottomPadding() + RippleMetrics.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.md)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.history_day_detail), style = MaterialTheme.typography.titleLarge)
                    Text(snapshot.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${UnitConverter.toDisplay(snapshot.summary.consumedMl, snapshot.preferredUnit)} ${snapshot.preferredUnit.symbol}", style = MaterialTheme.typography.displayMedium)
                    Text("of ${UnitConverter.toDisplay(snapshot.summary.goalMl, snapshot.preferredUnit)} ${snapshot.preferredUnit.symbol}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { LogButton(stringResource(R.string.history_add), onAdd, Modifier.fillMaxWidth()) }
        if (snapshot.entries.isEmpty()) {
            item { EmptyState(stringResource(R.string.history_no_entries), stringResource(R.string.history_empty_body)) }
        } else {
            items(snapshot.entries, key = { it.id }) { intake ->
                IntakeRow(intake, snapshot, onDelete, onRestore, onEdit)
            }
        }
    }
}

@Composable
private fun IntakeRow(
    intake: Intake,
    snapshot: DayDetailSnapshot,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onEdit: (Intake) -> Unit
) {
    GlassCardRow(
        title = "${UnitConverter.toDisplay(intake.amountMl, snapshot.preferredUnit)} ${snapshot.preferredUnit.symbol}",
        subtitle = intake.date.atZone(java.time.ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0).toString(),
        leading = { Icon(Icons.Outlined.Add, contentDescription = null) },
        trailing = {
            if (intake.isDeleted) {
                IconButton(onClick = { onRestore(intake.id) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.history_restore))
                }
            } else {
                IconButton(onClick = { onEdit(intake) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.history_edit))
                }
                IconButton(onClick = { onDelete(intake.id) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.history_delete))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditIntakeSheet(
    intake: Intake,
    unit: com.stefansturm.ripple.core.domain.VolumeUnit,
    onDismiss: () -> Unit,
    onSave: (Milliliters, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by rememberSaveable { mutableStateOf(intake.amountMl.value.toString()) }
    var note by rememberSaveable { mutableStateOf(intake.note.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RippleMetrics.xl),
            verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)
        ) {
            Text(stringResource(R.string.history_edit), style = MaterialTheme.typography.titleLarge)
            TextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.history_amount)) }, modifier = Modifier.fillMaxWidth())
            TextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.history_note)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.history_cancel)) }
                Button(onClick = { amount.toIntOrNull()?.let { onSave(Milliliters(it), note.ifBlank { null }) } }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.history_save)) }
            }
        }
    }
}
