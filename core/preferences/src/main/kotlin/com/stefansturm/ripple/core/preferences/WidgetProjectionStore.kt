package com.stefansturm.ripple.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stefansturm.ripple.core.domain.TodaySnapshot
import kotlinx.coroutines.flow.first

private val Context.widgetProjectionDataStore by preferencesDataStore(name = "widget_projection")

data class WidgetProjection(
    val consumedMl: Int = 0,
    val goalMl: Int = 2000,
    val remainingMl: Int = 2000,
    val defaultAddMl: Int = 250,
    val updatedAtMs: Long = 0L
)

class WidgetProjectionStore(private val context: Context) {
    suspend fun write(snapshot: TodaySnapshot) {
        context.widgetProjectionDataStore.edit { preferences ->
            preferences[CONSUMED] = snapshot.consumedMl.value
            preferences[GOAL] = snapshot.goalMl.value
            preferences[REMAINING] = snapshot.remainingMl.value
            preferences[DEFAULT_ADD] = snapshot.defaultAddMl.value
            preferences[UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun read(): WidgetProjection {
        val preferences = context.widgetProjectionDataStore.data.first()
        return WidgetProjection(
            consumedMl = preferences[CONSUMED] ?: 0,
            goalMl = preferences[GOAL] ?: 2000,
            remainingMl = preferences[REMAINING] ?: 2000,
            defaultAddMl = preferences[DEFAULT_ADD] ?: 250,
            updatedAtMs = preferences[UPDATED] ?: 0L
        )
    }

    private companion object {
        val CONSUMED = intPreferencesKey("consumed_ml")
        val GOAL = intPreferencesKey("goal_ml")
        val REMAINING = intPreferencesKey("remaining_ml")
        val DEFAULT_ADD = intPreferencesKey("default_add_ml")
        val UPDATED = longPreferencesKey("updated_at_ms")
    }
}
