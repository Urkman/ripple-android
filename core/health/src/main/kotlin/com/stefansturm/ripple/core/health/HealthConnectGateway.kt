package com.stefansturm.ripple.core.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.milliliters
import com.stefansturm.ripple.core.domain.Intake
import com.stefansturm.ripple.core.domain.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneOffset

class HealthConnectGateway(private val context: Context) {
    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    suspend fun permissionStatus(): PermissionStatus = withContext(Dispatchers.IO) {
        val healthClient = client ?: return@withContext PermissionStatus.UNAVAILABLE
        runCatching {
            val granted = healthClient.permissionController.getGrantedPermissions()
            when {
                requiredPermissions.all(granted::contains) -> PermissionStatus.GRANTED
                granted.isEmpty() -> PermissionStatus.NOT_DETERMINED
                else -> PermissionStatus.PARTIAL
            }
        }.getOrElse { PermissionStatus.FAILED }
    }

    suspend fun project(intake: Intake): PermissionStatus = withContext(Dispatchers.IO) {
        val healthClient = client ?: return@withContext PermissionStatus.UNAVAILABLE
        runCatching {
            val permissions = healthClient.permissionController.getGrantedPermissions()
            if (!requiredPermissions.all(permissions::contains)) return@withContext PermissionStatus.DENIED
            val offset = intake.date.atZone(java.time.ZoneId.systemDefault()).offset
            val record = HydrationRecord(
                startTime = intake.date,
                startZoneOffset = offset,
                endTime = intake.date.plusSeconds(1),
                endZoneOffset = offset,
                volume = intake.amountMl.value.toDouble().milliliters,
                metadata = Metadata.manualEntry()
            )
            healthClient.insertRecords(listOf(record))
            PermissionStatus.GRANTED
        }.getOrElse { PermissionStatus.FAILED }
    }

    fun settingsIntent(): Intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
        data = Uri.parse("package:${context.packageName}")
    }
}
