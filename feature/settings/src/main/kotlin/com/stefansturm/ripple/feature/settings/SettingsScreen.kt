package com.stefansturm.ripple.feature.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefansturm.ripple.core.designsystem.GlassCard
import com.stefansturm.ripple.core.designsystem.GlassCardRow
import com.stefansturm.ripple.core.designsystem.RippleMetrics
import com.stefansturm.ripple.core.designsystem.SectionLabel
import com.stefansturm.ripple.core.domain.ActivityLevel
import com.stefansturm.ripple.core.domain.ClockTime
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.ExportData
import com.stefansturm.ripple.core.domain.GoalMode
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.ReminderRule
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.UserProfile
import com.stefansturm.ripple.core.domain.VolumeUnit
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsRoute(
    repository: RippleRepository,
    onShare: (String, String) -> Unit,
    onOpenHealthConnect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(repository, state, viewModel, onShare, onOpenHealthConnect, modifier)
}

@Composable
private fun SettingsScreen(
    repository: RippleRepository,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onShare: (String, String) -> Unit,
    onOpenHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var containerEditor by remember { mutableStateOf<Container?>(null) }
    var addingContainer by remember { mutableStateOf(false) }
    var reminderEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Container?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(RippleMetrics.lg),
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.md)
    ) {
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) }
        item {
            SettingsSection(stringResource(R.string.settings_profile)) {
                Text(stringResource(R.string.settings_unit), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
                    UnitChip(VolumeUnit.MILLILITERS, state.profile.preferredUnit, viewModel::setUnit)
                    UnitChip(VolumeUnit.FLUID_OUNCES, state.profile.preferredUnit, viewModel::setUnit)
                }
                Text(stringResource(R.string.settings_activity), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
                    ActivityChip(ActivityLevel.SEDENTARY, state.profile.activityLevel, viewModel::setActivity)
                    ActivityChip(ActivityLevel.MODERATE, state.profile.activityLevel, viewModel::setActivity)
                    ActivityChip(ActivityLevel.HIGH, state.profile.activityLevel, viewModel::setActivity)
                }
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_goal)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_manual_goal), modifier = Modifier.weight(1f))
                    Switch(checked = state.goal.mode == GoalMode.MANUAL, onCheckedChange = { viewModel.setGoalMode(if (it) GoalMode.MANUAL else GoalMode.CALCULATED) })
                }
                if (state.goal.mode == GoalMode.MANUAL) {
                    var goal by remember(state.goal.manualGoalMl.value) { mutableIntStateOf(state.goal.manualGoalMl.value) }
                    Text("${goal} ${stringResource(R.string.settings_ml)}", style = MaterialTheme.typography.titleMedium)
                    Slider(value = goal.toFloat(), onValueChange = { goal = (it / 50).toInt() * 50 }, valueRange = 500f..5000f, steps = 89, onValueChangeFinished = { viewModel.setManualGoal(goal) })
                } else {
                    Text(stringResource(R.string.settings_calculated_goal), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_containers)) {
                state.containers.forEach { container ->
                    GlassCardRow(
                        title = "${container.name} · ${container.amountMl.value} ml",
                        subtitle = if (container.isDefault) stringResource(R.string.settings_default) else null,
                        leading = { Icon(Icons.Outlined.WaterDrop, contentDescription = null) },
                        trailing = {
                            IconButton(onClick = { containerEditor = container }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.settings_edit)) }
                            IconButton(onClick = { deleteCandidate = container }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.settings_delete)) }
                        }
                    )
                }
                OutlinedButton(onClick = { addingContainer = true }, modifier = Modifier.fillMaxWidth().heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(stringResource(R.string.settings_add_container), modifier = Modifier.padding(start = RippleMetrics.sm))
                }
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_reminders_enabled), modifier = Modifier.weight(1f))
                    Switch(checked = state.reminder.enabled, onCheckedChange = { viewModel.saveReminder(state.reminder.copy(enabled = it)) })
                }
                OutlinedButton(onClick = { reminderEditor = true }, modifier = Modifier.fillMaxWidth().heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_edit)) }
            }
        }
        item {
            SettingsSection(stringResource(R.string.settings_health)) {
                Text(stringResource(R.string.settings_health_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onOpenHealthConnect, modifier = Modifier.fillMaxWidth().heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_connect)) }
            }
        }
        item { SettingsSection(stringResource(R.string.settings_sync)) { Text(stringResource(R.string.settings_sync_body), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item {
            SettingsSection(stringResource(R.string.settings_export)) {
                Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { export(scope, repository, true, onShare) }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_export_json)) }
                    OutlinedButton(onClick = { export(scope, repository, false, onShare) }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_export_csv)) }
                }
            }
        }
        item { SettingsSection(stringResource(R.string.settings_about)) { Text(stringResource(R.string.settings_about_body), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
    containerEditor?.let { existing ->
        ContainerEditorSheet(existing, state.containers, onDismiss = { containerEditor = null }, onSave = { saved -> viewModel.saveContainer(saved); containerEditor = null })
    }
    if (addingContainer) {
        ContainerEditorSheet(null, state.containers, onDismiss = { addingContainer = false }, onSave = { saved -> viewModel.saveContainer(saved); addingContainer = false })
    }
    if (reminderEditor) {
        ReminderEditorSheet(state.reminder, onDismiss = { reminderEditor = false }, onSave = { rule -> viewModel.saveReminder(rule); reminderEditor = false })
    }
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.settings_delete_question)) },
            text = { Text(stringResource(R.string.settings_delete_body)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteContainer(candidate.id); deleteCandidate = null }) { Text(stringResource(R.string.settings_delete)) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.settings_cancel)) } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RippleMetrics.md)) {
        SectionLabel(title)
        GlassCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun UnitChip(unit: VolumeUnit, selected: VolumeUnit, onSelected: (VolumeUnit) -> Unit) {
    FilterChip(selected = unit == selected, onClick = { onSelected(unit) }, label = { Text(unit.symbol) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin))
}

@Composable
private fun ActivityChip(activity: ActivityLevel, selected: ActivityLevel, onSelected: (ActivityLevel) -> Unit) {
    val text = when (activity) {
        ActivityLevel.SEDENTARY -> stringResource(R.string.settings_sedentary)
        ActivityLevel.MODERATE -> stringResource(R.string.settings_moderate)
        ActivityLevel.HIGH -> stringResource(R.string.settings_high)
    }
    FilterChip(selected = activity == selected, onClick = { onSelected(activity) }, label = { Text(text) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ContainerEditorSheet(
    existing: Container?,
    containers: List<Container>,
    onDismiss: () -> Unit,
    onSave: (Container) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.amountMl?.value ?: 250) }
    var isDefault by rememberSaveable(existing?.id) { mutableStateOf(existing?.isDefault ?: containers.none { it.isDefault }) }
    var symbol by rememberSaveable(existing?.id) { mutableStateOf(existing?.symbol ?: "glass") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.xl), verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)) {
            Text(stringResource(if (existing == null) R.string.settings_add_container else R.string.settings_edit_container), style = MaterialTheme.typography.titleLarge)
            TextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.settings_container_name)) }, modifier = Modifier.fillMaxWidth())
            Text("${stringResource(R.string.settings_container_amount)}: $amount ml", style = MaterialTheme.typography.labelLarge)
            Slider(value = amount.toFloat(), onValueChange = { amount = (it / 10).toInt() * 10 }, valueRange = 50f..2000f, steps = 194)
            Text(stringResource(R.string.settings_icon), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)) {
                listOf("glass", "cup", "bottle").forEach { option ->
                    FilterChip(selected = symbol == option, onClick = { symbol = option }, label = { Text(option) }, modifier = Modifier.heightIn(min = RippleMetrics.controlMin))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_default), modifier = Modifier.weight(1f))
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_cancel)) }
                Button(onClick = {
                    onSave(existing ?: Container(UUID.randomUUID().toString(), name, Milliliters(amount), isDefault, containers.size, symbol, false, Instant.now(), Instant.now()))
                }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_save)) }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorSheet(rule: ReminderRule, onDismiss: () -> Unit, onSave: (ReminderRule) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var start by remember { mutableStateOf(rule.start) }
    var end by remember { mutableStateOf(rule.end) }
    var interval by rememberSaveable { mutableIntStateOf(rule.intervalMinutes) }
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.xl), verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)) {
            Text(stringResource(R.string.settings_reminders), style = MaterialTheme.typography.titleLarge)
            TimeRow(stringResource(R.string.settings_start), start) { TimePickerDialog(context, { _, hour, minute -> start = ClockTime(hour, minute) }, start.hour, start.minute, true).show() }
            TimeRow(stringResource(R.string.settings_end), end) { TimePickerDialog(context, { _, hour, minute -> end = ClockTime(hour, minute) }, end.hour, end.minute, true).show() }
            Text("${stringResource(R.string.settings_interval)}: $interval ${stringResource(R.string.settings_minutes)}")
            Slider(value = interval.toFloat(), onValueChange = { interval = (it / 10).toInt() * 10 }, valueRange = 30f..240f, steps = 20)
            Row(horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_cancel)) }
                Button(onClick = { onSave(rule.copy(start = start, end = end, intervalMinutes = interval)) }, modifier = Modifier.weight(1f).heightIn(min = RippleMetrics.controlMin)) { Text(stringResource(R.string.settings_save)) }
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, time: ClockTime, onClick: () -> Unit) {
    GlassCardRow(title = label, subtitle = "%02d:%02d".format(time.hour, time.minute), onClick = onClick)
}

private fun export(scope: CoroutineScope, repository: RippleRepository, json: Boolean, onShare: (String, String) -> Unit) {
    // The screen delegates preparation to the domain export operation; the host owns the Android chooser.
    scope.launch {
        ExportData(repository)(true).getOrNull()?.let { payload ->
            onShare(if (json) payload.json else payload.csv, if (json) "application/json" else "text/csv")
        }
    }
}
