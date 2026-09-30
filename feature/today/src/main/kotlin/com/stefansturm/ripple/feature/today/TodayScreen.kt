package com.stefansturm.ripple.feature.today

import android.view.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefansturm.ripple.core.designsystem.AmountReadout
import com.stefansturm.ripple.core.designsystem.EmptyState
import com.stefansturm.ripple.core.designsystem.GlassCard
import com.stefansturm.ripple.core.designsystem.HorizontalContainerChips
import com.stefansturm.ripple.core.designsystem.LocalRippleReducedMotion
import com.stefansturm.ripple.core.designsystem.LogButton
import com.stefansturm.ripple.core.designsystem.QuickAddCluster
import com.stefansturm.ripple.core.designsystem.RippleHero
import com.stefansturm.ripple.core.designsystem.RippleMetrics
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.RippleRepository
import com.stefansturm.ripple.core.domain.TodaySnapshot
import com.stefansturm.ripple.core.domain.UnitConverter
import com.stefansturm.ripple.core.domain.VolumeUnit
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TodayRoute(
    repository: RippleRepository,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        onQuickAdd = { viewModel.log(it.amountMl, it.id) },
        onCustomAdd = { amount, containerId -> viewModel.log(amount, containerId) },
        onUndo = viewModel::undo,
        onClearMessage = viewModel::clearMessage,
        visualEvent = uiState.visualEvent,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
private fun TodayScreen(
    uiState: TodayUiState,
    onQuickAdd: (Container) -> Unit,
    onCustomAdd: (Milliliters, String?) -> Unit,
    onUndo: () -> Unit,
    onClearMessage: () -> Unit,
    visualEvent: TodayVisualEvent?,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCustomAmount by rememberSaveable { mutableStateOf(false) }
    var heroAnimating by remember { mutableStateOf(false) }
    val snapshot = uiState.snapshot
    val loadingDescription = stringResource(R.string.today_loading)
    val undoLabel = stringResource(R.string.today_undo)
    val successMessage = uiState.successAmountMl?.let { amount ->
        val unit = snapshot?.preferredUnit ?: VolumeUnit.MILLILITERS
        stringResource(
            R.string.today_added,
            "${UnitConverter.toDisplay(amount, unit)} ${unit.symbol}"
        )
    }
    val feedbackMessage = uiState.message ?: successMessage
    LaunchedEffect(feedbackMessage, uiState.canUndo, heroAnimating) {
        if (feedbackMessage != null && (!heroAnimating || !uiState.canUndo)) {
            val result = snackbarHostState.showSnackbar(
                message = feedbackMessage,
                actionLabel = if (uiState.canUndo) undoLabel else null
            )
            if (result == SnackbarResult.ActionPerformed && uiState.canUndo) onUndo()
            onClearMessage()
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
            }
        } else {
            TodayContent(
                snapshot = snapshot,
                visualEvent = visualEvent,
                contentPadding = padding,
                onQuickAdd = onQuickAdd,
                onCustomAmount = { showCustomAmount = true },
                onAnimationStateChanged = { heroAnimating = it },
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings
            )
        }
    }
    if (showCustomAmount && snapshot != null) {
        CustomAmountSheet(
            snapshot = snapshot,
            onDismiss = { showCustomAmount = false },
            onAdd = { amount, containerId ->
                showCustomAmount = false
                onCustomAdd(amount, containerId)
            }
        )
    }
}

@Composable
private fun TodayContent(
    snapshot: TodaySnapshot,
    visualEvent: TodayVisualEvent?,
    contentPadding: PaddingValues,
    onQuickAdd: (Container) -> Unit,
    onCustomAmount: () -> Unit,
    onAnimationStateChanged: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reducedMotion = LocalRippleReducedMotion.current
    val tiltController = remember(context, view) {
        AndroidTiltController(context) { view.display?.rotation ?: Surface.ROTATION_0 }
    }
    val tiltRadians by tiltController.tilt.collectAsStateWithLifecycle()
    DisposableEffect(lifecycleOwner, tiltController, reducedMotion) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!reducedMotion) tiltController.start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> tiltController.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!reducedMotion && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            tiltController.start()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tiltController.stop()
        }
    }
    val heroMotion = rememberTodayHeroMotion(
        snapshot = snapshot,
        visualEvent = visualEvent,
        tiltRadians = tiltRadians,
        reducedMotion = reducedMotion,
        onAnimationStateChanged = onAnimationStateChanged
    )
    val unit = snapshot.preferredUnit
    val displayedConsumedMl = heroMotion.displayedConsumedMl
    val displayedRemainingMl = Milliliters((snapshot.goalMl.value - displayedConsumedMl.value).coerceAtLeast(0))
    val displayedPercent = if (snapshot.goalMl.value <= 0) 0 else ((displayedConsumedMl.value * 100f) / snapshot.goalMl.value).toInt()
    val dateText = snapshot.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    val heroAccessibility = stringResource(
        R.string.today_hero_accessibility,
        UnitConverter.toDisplay(displayedConsumedMl, unit),
        unit.symbol,
        UnitConverter.toDisplay(snapshot.goalMl, unit),
        UnitConverter.toDisplay(displayedRemainingMl, unit),
        displayedPercent.coerceIn(0, 100)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = RippleMetrics.lg,
            top = contentPadding.calculateTopPadding() + RippleMetrics.sm,
            end = RippleMetrics.lg,
            bottom = contentPadding.calculateBottomPadding() + RippleMetrics.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.today_product_name), style = MaterialTheme.typography.titleLarge)
                    Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.today_history))
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.today_settings))
                }
            }
        }
        item {
            RippleHero(
                consumedMl = displayedConsumedMl,
                goalMl = snapshot.goalMl,
                unit = unit,
                visualProgress = heroMotion.level,
                tiltRadians = heroMotion.tiltRadians,
                pourProgress = heroMotion.pourProgress,
                pourAddedMl = heroMotion.pourAddedMl,
                surfaceAmplitude = heroMotion.surfaceAmplitude,
                accessibilityDescription = heroAccessibility,
                modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 390.dp)
            )
        }
        item {
            val remainingText = if (displayedConsumedMl.value > snapshot.goalMl.value) {
                stringResource(R.string.today_goal_over)
            } else if (displayedRemainingMl.value == 0) {
                stringResource(R.string.today_goal_reached)
            } else {
                stringResource(
                    R.string.today_remaining,
                    "${UnitConverter.toDisplay(displayedRemainingMl, unit)} ${unit.symbol}"
                )
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(RippleMetrics.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(remainingText, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${displayedPercent.coerceIn(0, 100)}% of ${UnitConverter.toDisplay(snapshot.goalMl, unit)} ${unit.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            QuickAddCluster(snapshot.containers, unit, onQuickAdd)
        }
        item {
            LogButton(stringResource(R.string.today_custom_amount), onCustomAmount, Modifier.fillMaxWidth())
        }
        if (snapshot.entries.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.today_empty_title),
                    body = stringResource(R.string.today_empty_body)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAmountSheet(
    snapshot: TodaySnapshot,
    onDismiss: () -> Unit,
    onAdd: (Milliliters, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by rememberSaveable { mutableFloatStateOf(snapshot.defaultAddMl.value.toFloat()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(snapshot.containers.firstOrNull()?.id) }
    val customTitle = stringResource(R.string.today_custom_title)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.semantics { contentDescription = customTitle }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(RippleMetrics.xl),
            verticalArrangement = Arrangement.spacedBy(RippleMetrics.lg)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.today_custom_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.today_dismiss)) }
                }
            }
            item { AmountReadout(Milliliters(amount.toInt()), snapshot.preferredUnit) }
            item {
                Text(stringResource(R.string.today_amount), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = amount,
                    onValueChange = { amount = (it / 10f).roundToInt() * 10f },
                    valueRange = 50f..2000f,
                    steps = 194,
                    colors = SliderDefaults.colors()
                )
            }
            item {
                HorizontalContainerChips(snapshot.containers, snapshot.preferredUnit, selectedId, { selectedId = it.id })
            }
            item {
                LogButton(
                    text = stringResource(R.string.today_add),
                    onClick = { onAdd(Milliliters(amount.toInt()), selectedId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
