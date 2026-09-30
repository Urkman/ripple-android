package com.stefansturm.ripple.wear

import android.content.Context
import androidx.core.content.edit
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.stefansturm.ripple.core.wear.sync.WearMutationEnvelope
import com.stefansturm.ripple.core.wear.sync.WearSyncPaths

/** The outbox contains sync projections only; Room remains the local product source of truth. */
class WearOutboxStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ripple_wear_sync",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun enqueue(mutation: WearMutationEnvelope) {
        val ids = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids += mutation.mutationId
        preferences.edit {
            putStringSet(KEY_IDS, ids)
            putString(KEY_PAYLOAD_PREFIX + mutation.mutationId, mutation.toJson())
        }
    }

    @Synchronized
    fun acknowledge(mutationId: String) {
        val ids = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids.remove(mutationId)
        preferences.edit {
            putStringSet(KEY_IDS, ids)
            remove(KEY_PAYLOAD_PREFIX + mutationId)
        }
    }

    fun pendingCount(): Int = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().size

    private companion object {
        const val KEY_IDS = "pending_ids"
        const val KEY_PAYLOAD_PREFIX = "mutation_"
    }
}

class WearSyncClient(context: Context, private val outbox: WearOutboxStore) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    fun enqueueAndSend(mutation: WearMutationEnvelope) {
        outbox.enqueue(mutation)
        val request = PutDataRequest.create(WearSyncPaths.MUTATIONS_OUTGOING)
            .setUrgent()
            .setData(mutation.toJson().toByteArray())
        dataClient.putDataItem(request)
    }
}
