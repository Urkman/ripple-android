package com.stefansturm.ripple.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        IntakeEntity::class,
        ContainerEntity::class,
        ProfileEntity::class,
        GoalEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RippleDatabase : RoomDatabase() {
    abstract fun rippleDao(): RippleDao
}
