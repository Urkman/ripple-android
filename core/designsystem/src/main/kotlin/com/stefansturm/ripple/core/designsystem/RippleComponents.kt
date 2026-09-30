package com.stefansturm.ripple.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.DaySummary
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.UnitConverter
import com.stefansturm.ripple.core.domain.VolumeUnit
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(RippleMetrics.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun GlassCardRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RippleMetrics.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md)
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun AmountReadout(
    amountMl: Milliliters,
    unit: VolumeUnit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    val amount = UnitConverter.toDisplay(amountMl, unit)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = unit.symbol,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RippleHero(
    consumedMl: Milliliters,
    goalMl: Milliliters,
    unit: VolumeUnit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    visualProgress: Float? = null,
    tiltRadians: Float = 0f,
    pourProgress: Float? = null,
    pourAddedMl: Int = 0,
    surfaceAmplitude: Float = 0f,
    accessibilityDescription: String? = null
) {
    val progress = if (goalMl.value <= 0) 0f else (consumedMl.value.toFloat() / goalMl.value).coerceIn(0f, 1f)
    val level = (visualProgress ?: progress).coerceIn(0f, 1f)
    val description = accessibilityDescription
        ?: "${UnitConverter.toDisplay(consumedMl, unit)} ${unit.symbol} consumed of ${UnitConverter.toDisplay(goalMl, unit)} ${unit.symbol} goal"
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRippleHero(
                level = level,
                tiltRadians = tiltRadians,
                pourProgress = pourProgress,
                pourAddedMl = pourAddedMl,
                surfaceAmplitude = surfaceAmplitude
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = UnitConverter.toDisplay(consumedMl, unit).toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(unit.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRippleHero(
    level: Float,
    tiltRadians: Float,
    pourProgress: Float?,
    pourAddedMl: Int,
    surfaceAmplitude: Float
) {
    val stroke = RippleMetrics.heroStroke.toPx()
    val glassHeight = min(size.height * 0.9f, RippleMetrics.heroMaxHeight.toPx())
    val glassWidth = min(size.width * RippleMetrics.heroWidthFraction, RippleMetrics.heroMaxWidth.toPx())
    val top = (size.height - glassHeight) / 2f
    val centerX = size.width / 2f
    val outerBaseWidth = glassWidth / RippleMetrics.heroRimToBaseRatio
    val outer = tumblerPath(
        centerX = centerX,
        top = top,
        height = glassHeight,
        rimWidth = glassWidth,
        baseWidth = outerBaseWidth,
        bottomRadius = RippleMetrics.heroBottomRadius.toPx()
    )
    val innerTop = top + stroke * 1.7f
    val innerBottom = top + glassHeight - stroke * 1.7f
    val inner = tumblerPath(
        centerX = centerX,
        top = innerTop,
        height = innerBottom - innerTop,
        rimWidth = glassWidth - stroke * 3.4f,
        baseWidth = outerBaseWidth - stroke * 3.4f,
        bottomRadius = RippleMetrics.heroBottomRadius.toPx() - stroke
    )
    val innerTopLeft = centerX - (glassWidth - stroke * 3.4f) / 2f
    val innerTopRight = centerX + (glassWidth - stroke * 3.4f) / 2f
    val innerBottomLeft = centerX - (outerBaseWidth - stroke * 3.4f) / 2f
    val innerBottomRight = centerX + (outerBaseWidth - stroke * 3.4f) / 2f

    drawPath(outer, color = RippleColors.surface)
    drawPourStream(
        pourProgress = pourProgress,
        pourAddedMl = pourAddedMl,
        top = innerTop,
        centerX = centerX,
        surfaceY = waterSurfaceY(level, innerTop, innerBottom)
    )

    if (level > 0.001f) {
        val surfaceY = waterSurfaceY(level, innerTop, innerBottom)
        val waterPath = Path()
        val surfacePath = Path()
        val points = 36
        for (index in 0..points) {
            val fraction = index / points.toFloat()
            val x = lerp(innerTopLeft, innerTopRight, fraction)
            val baseY = surfaceY + sin(tiltRadians) * glassWidth * 0.35f * (fraction - 0.5f)
            val wave = sin(fraction * Math.PI).toFloat() * surfaceAmplitude * glassHeight
            val y = baseY + wave
            if (index == 0) {
                waterPath.moveTo(x, y)
                surfacePath.moveTo(x, y)
            } else {
                waterPath.lineTo(x, y)
                surfacePath.lineTo(x, y)
            }
        }
        waterPath.lineTo(innerBottomRight, innerBottom)
        waterPath.quadraticTo(centerX, innerBottom + RippleMetrics.heroBottomRadius.toPx() * 0.35f, innerBottomLeft, innerBottom)
        waterPath.close()
        clipPath(inner) {
            drawPath(waterPath, color = RippleColors.aqua.copy(alpha = 0.76f))
            drawPath(
                surfacePath,
                color = RippleColors.lagoon,
                style = Stroke(width = RippleMetrics.heroWaterStroke.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    drawPath(
        outer,
        color = RippleColors.foam.copy(alpha = 0.95f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawPath(
        inner,
        color = RippleColors.foam.copy(alpha = 0.8f),
        style = Stroke(width = RippleMetrics.heroInnerStroke.toPx(), cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPourStream(
    pourProgress: Float?,
    pourAddedMl: Int,
    top: Float,
    centerX: Float,
    surfaceY: Float
) {
    if (pourProgress == null) return
    val amountT = ((pourAddedMl - 50) / 700f).coerceIn(0f, 1f)
    val activeDuration = 400f + 300f * amountT
    val totalDuration = activeDuration + 280f
    val elapsed = (pourProgress.coerceIn(0f, 1f) * totalDuration)
    val leadIn = 140f
    val fadeStart = leadIn + activeDuration
    val travel = (elapsed / leadIn).coerceIn(0f, 1f)
    val opacity = if (elapsed <= fadeStart) 1f else 1f - ((elapsed - fadeStart) / 140f).coerceIn(0f, 1f)
    if (opacity <= 0f) return
    val streamTop = top - RippleMetrics.pourStreamAboveGlass.toPx()
    val streamBottom = lerp(streamTop, surfaceY, travel)
    val streamWidth = 7.dp.toPx() + 5.dp.toPx() * amountT
    drawRoundRect(
        color = RippleColors.aqua.copy(alpha = 0.9f * opacity),
        topLeft = Offset(centerX - streamWidth / 2f, streamTop),
        size = Size(streamWidth, (streamBottom - streamTop).coerceAtLeast(0f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(streamWidth / 2f, streamWidth / 2f)
    )
}

private fun tumblerPath(
    centerX: Float,
    top: Float,
    height: Float,
    rimWidth: Float,
    baseWidth: Float,
    bottomRadius: Float
): Path {
    val left = centerX - rimWidth / 2f
    val right = centerX + rimWidth / 2f
    val baseLeft = centerX - baseWidth / 2f
    val baseRight = centerX + baseWidth / 2f
    val bottom = top + height
    val radius = min(bottomRadius, height * 0.18f)
    return Path().apply {
        moveTo(left, top + radius * 0.25f)
        quadraticTo(centerX, top - radius * 0.25f, right, top + radius * 0.25f)
        lineTo(baseRight, bottom - radius)
        quadraticTo(baseRight, bottom, centerX, bottom)
        quadraticTo(baseLeft, bottom, baseLeft, bottom - radius)
        lineTo(left, top + radius * 0.25f)
        close()
    }
}

private fun waterSurfaceY(level: Float, top: Float, bottom: Float): Float = lerp(bottom, top, level.coerceIn(0f, 1f))

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction.coerceIn(0f, 1f)

@Composable
fun QuickAddCluster(
    containers: List<Container>,
    unit: VolumeUnit,
    onContainerClick: (Container) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RippleMetrics.md)
    ) {
        containers.take(3).forEach { container ->
            QuickAddButton(container = container, unit = unit, onClick = { onContainerClick(container) })
        }
    }
}

@Composable
private fun RowScope.QuickAddButton(container: Container, unit: VolumeUnit, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(RippleMetrics.controlMin),
        shape = RoundedCornerShape(RippleMetrics.controlRadius),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = RippleMetrics.sm)
    ) {
        Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(RippleMetrics.smallIcon))
        Spacer(Modifier.width(RippleMetrics.xs))
        Text(UnitConverter.toDisplay(container.amountMl, unit).toString() + " " + unit.symbol)
    }
}

@Composable
fun ContainerChip(
    container: Container,
    unit: VolumeUnit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = { Text("${container.name} · ${UnitConverter.toDisplay(container.amountMl, unit)} ${unit.symbol}") },
        leadingIcon = {
            Icon(
                if (selected) Icons.Outlined.Check else Icons.Outlined.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(RippleMetrics.smallIcon)
            )
        },
        modifier = modifier.heightIn(min = RippleMetrics.controlMin),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun HorizontalContainerChips(
    containers: List<Container>,
    unit: VolumeUnit,
    selectedId: String?,
    onSelected: (Container) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RippleMetrics.sm)
    ) {
        containers.forEach { container ->
            ContainerChip(container, unit, selectedId == container.id, { onSelected(container) })
        }
    }
}

@Composable
fun LogButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = RippleMetrics.controlMin),
        shape = RoundedCornerShape(RippleMetrics.controlRadius)
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(RippleMetrics.sm))
        Text(text)
    }
}

@Composable
fun DayRing(
    summary: DaySummary,
    modifier: Modifier = Modifier,
    today: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val progress = summary.cappedProgress
    val description = "${summary.date}: ${summary.consumedMl.value} ml of ${summary.goalMl.value} ml"
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RippleMetrics.controlRadius))
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(RippleMetrics.sm)
            .semantics {
                contentDescription = description
                stateDescription = if (today) "Today" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.xs)
    ) {
        Canvas(Modifier.size(42.dp)) {
            drawArc(trackColor, -90f, 360f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            drawArc(progressColor, -90f, 360f * progress, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(summary.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyState(title: String, body: String, actionText: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(RippleMetrics.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RippleMetrics.md)
    ) {
        Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = RippleMetrics.controlMin)) { Text(actionText) }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = RippleMetrics.xs),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
