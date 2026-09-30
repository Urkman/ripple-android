package com.stefansturm.ripple.core.wear.sync

import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.IntakeSource
import java.time.Instant
import java.util.UUID

object WearSyncPaths {
    const val SNAPSHOT_REQUEST = "/ripple/v1/snapshot/request"
    const val SNAPSHOT_RESPONSE = "/ripple/v1/snapshot/response"
    const val MUTATIONS_OUTGOING = "/ripple/v1/mutations/outgoing"
    const val MUTATIONS_ACK = "/ripple/v1/mutations/ack"
    const val STATUS = "/ripple/v1/status"
}

enum class WearMutationOperation { INSERT, UPDATE, DELETE, RESTORE }

data class WearMutationEnvelope(
    val schemaVersion: Int,
    val mutationId: String,
    val entityId: String,
    val entityType: String,
    val operation: WearMutationOperation,
    val payload: String,
    val updatedAt: Instant,
    val originDeviceId: String
) {
    fun toJson(): String = buildString {
        append('{')
        appendJsonField("schemaVersion", schemaVersion.toString(), raw = true)
        appendJsonField("mutationId", mutationId)
        appendJsonField("entityId", entityId)
        appendJsonField("entityType", entityType)
        appendJsonField("operation", operation.name.lowercase())
        appendJsonField("payload", payload, raw = true)
        appendJsonField("updatedAt", updatedAt.toString())
        appendJsonField("originDeviceId", originDeviceId)
        append('}')
    }

    private fun StringBuilder.appendJsonField(name: String, value: String, raw: Boolean = false) {
        if (length > 1) append(',')
        append('"').append(name).append("\":")
        if (raw) append(value) else append('"').append(value.escapeJson()).append('"')
    }
}

object WearMutationFactory {
    fun forIntake(
        intake: Intake,
        originDeviceId: String,
        operationOverride: WearMutationOperation? = null
    ): WearMutationEnvelope =
        WearMutationEnvelope(
            schemaVersion = 1,
            mutationId = UUID.randomUUID().toString(),
            entityId = intake.id,
            entityType = "intake",
            operation = operationOverride ?: if (intake.isDeleted) WearMutationOperation.DELETE else WearMutationOperation.INSERT,
            payload = buildString {
                append("{\"id\":\"").append(intake.id.escapeJson())
                    .append("\",\"amountMl\":").append(intake.amountMl.value)
                    .append(",\"date\":\"").append(intake.date).append("\"")
                    .append(",\"source\":\"").append(IntakeSource.WATCH.name.lowercase()).append("\"")
                    .append(",\"containerId\":")
                    .append(intake.containerId?.let { "\"${it.escapeJson()}\"" } ?: "null")
                    .append(",\"deleted\":").append(intake.isDeleted)
                    .append('}')
            },
            updatedAt = intake.updatedAt,
            originDeviceId = originDeviceId
        )
}

private fun String.escapeJson(): String = replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
