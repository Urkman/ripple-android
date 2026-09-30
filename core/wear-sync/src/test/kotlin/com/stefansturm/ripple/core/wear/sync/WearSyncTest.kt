package com.stefansturm.ripple.core.wear.sync

import com.stefansturm.ripple.core.domain.Container
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.Milliliters
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSyncTest {
    @Test
    fun intakeEnvelopeKeepsStableIdentityAndRawPayload() {
        val now = Instant.parse("2026-09-25T09:00:00Z")
        val envelope = WearMutationFactory.forIntake(
            Intake(
                id = "intake-1",
                date = now,
                amountMl = Milliliters(250),
                source = IntakeSource.WATCH,
                containerId = "glass",
                createdAt = now,
                updatedAt = now
            ),
            originDeviceId = "wear-a"
        )

        val json = envelope.toJson()
        assertTrue(json.contains("\"entityId\":\"intake-1\""))
        assertTrue(json.contains("\"payload\":{\"id\":\"intake-1\""))
        assertTrue(json.contains("\"amountMl\":250"))
    }
}
