package com.stefansturm.ripple.system.widgets

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.stefansturm.ripple.core.preferences.WidgetProjection
import com.stefansturm.ripple.core.preferences.WidgetProjectionStore

class RippleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val projection = WidgetProjectionStore(context).read()
        val labels = WidgetLabels(
            consumed = context.getString(R.string.widget_consumed, projection.consumedMl),
            goal = context.getString(R.string.widget_goal, projection.goalMl),
            remaining = context.getString(R.string.widget_remaining, projection.remainingMl),
            add = context.getString(R.string.widget_add, projection.defaultAddMl),
            unavailable = context.getString(R.string.widget_unavailable),
            stale = context.getString(R.string.widget_stale)
        )
        provideContent { WidgetContent(projection, labels) }
    }

    @Composable
    private fun WidgetContent(projection: WidgetProjection, labels: WidgetLabels) {
        val launch = ComponentName("de.stefansturm.ripple", "com.stefansturm.ripple.MainActivity")
        val status = when {
            projection.updatedAtMs == 0L -> labels.unavailable
            System.currentTimeMillis() - projection.updatedAtMs > FRESHNESS_WINDOW_MS -> labels.stale
            else -> null
        }
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(0xFFE8F4F6.toInt())).padding(16).clickable(actionStartActivity(launch)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(labels.consumed)
            Text(labels.goal)
            Spacer(GlanceModifier.width(4))
            Row { Text(labels.remaining) }
            Text(labels.add, modifier = GlanceModifier.clickable(actionRunCallback<LogDefaultAction>()))
            status?.let { Text(it) }
        }
    }

    private companion object {
        const val FRESHNESS_WINDOW_MS = 24L * 60L * 60L * 1_000L
    }
}

private data class WidgetLabels(
    val consumed: String,
    val goal: String,
    val remaining: String,
    val add: String,
    val unavailable: String,
    val stale: String
)

class LogDefaultAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        context.startActivity(
            Intent("de.stefansturm.ripple.LOG_DEFAULT")
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

class RippleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RippleWidget()
}
