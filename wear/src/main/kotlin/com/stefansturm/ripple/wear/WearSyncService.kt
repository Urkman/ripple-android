package com.stefansturm.ripple.wear

import android.net.Uri
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.stefansturm.ripple.core.wear.sync.WearSyncPaths

class WearSyncService : WearableListenerService() {
    private lateinit var outbox: WearOutboxStore

    override fun onCreate() {
        super.onCreate()
        outbox = WearOutboxStore(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path ?: return@forEach
            if (path != WearSyncPaths.MUTATIONS_ACK) return@forEach
            val payload = event.dataItem.data?.toString(Charsets.UTF_8).orEmpty()
            extractMutationId(payload)?.let(outbox::acknowledge)
        }
    }

    private fun extractMutationId(payload: String): String? {
        val marker = "\"mutationId\":\""
        val start = payload.indexOf(marker)
        if (start < 0) return null
        val valueStart = start + marker.length
        val end = payload.indexOf('"', valueStart)
        return payload.substring(valueStart, end.takeIf { it >= 0 } ?: payload.length)
    }
}
