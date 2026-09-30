package com.stefansturm.ripple.wear.complications

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.stefansturm.ripple.core.storage.RoomRippleRepository
import com.stefansturm.ripple.core.storage.createRippleDatabase
import com.stefansturm.ripple.wear.WearMainActivity
import kotlinx.coroutines.flow.first

class RippleComplicationService : SuspendingComplicationDataSourceService() {
    private val repository by lazy { RoomRippleRepository(createRippleDatabase(this)) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val snapshot = runCatching { repository.observeToday().first() }.getOrNull()
        return buildData(snapshot?.remainingMl?.value?.toString() ?: "—")
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData = buildData("1.2L")

    private fun buildData(value: String): ComplicationData {
        val text = PlainComplicationText.Builder(value.take(7)).build()
        val description = PlainComplicationText.Builder("Ripple remaining goal").build()
        return ShortTextComplicationData.Builder(text, description)
            .setTitle(PlainComplicationText.Builder("left").build())
            .setTapAction(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, WearMainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override fun onDestroy() {
        repository // Force lazy initialization ownership to remain explicit for the service lifetime.
        super.onDestroy()
    }
}
