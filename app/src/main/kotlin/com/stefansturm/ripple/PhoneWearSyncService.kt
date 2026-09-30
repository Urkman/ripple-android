package com.stefansturm.ripple

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.wear.sync.WearSyncPaths
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.json.JSONObject

class PhoneWearSyncService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            if (event.dataItem.uri.path != WearSyncPaths.MUTATIONS_OUTGOING) return@forEach
            val payload = event.dataItem.data?.toString(Charsets.UTF_8) ?: return@forEach
            serviceScope.launch { applyMutation(payload) }
        }
    }

    private suspend fun applyMutation(serialized: String) {
        val root = runCatching { JSONObject(serialized) }.getOrNull() ?: return
        val mutationId = root.optString("mutationId")
        val entityId = root.optString("entityId")
        val operation = root.optString("operation")
        val payload = root.optJSONObject("payload") ?: return
        val repository = (application as RippleApplication).graph.repository
        runCatching {
            when (operation) {
                "insert", "update" -> repository.logIntake(
                    LogIntakeCommand(
                        amountMl = Milliliters(payload.getInt("amountMl")),
                        source = IntakeSource.fromRaw(payload.optString("source")),
                        date = Instant.parse(payload.getString("date")),
                        containerId = payload.optString("containerId").takeIf { it.isNotBlank() && it != "null" },
                        explicitId = entityId
                    )
                )
                "delete" -> repository.deleteIntake(entityId)
                "restore" -> repository.restoreIntake(entityId)
            }
        }.onSuccess {
            val ack = JSONObject().put("mutationId", mutationId).toString()
            Wearable.getDataClient(this).putDataItem(
                PutDataRequest.create(WearSyncPaths.MUTATIONS_ACK)
                    .setUrgent()
                    .setData(ack.toByteArray())
            )
        }
    }

    override fun onDestroy() {
        serviceScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
