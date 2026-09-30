package com.stefansturm.ripple.core.storage

import android.content.Context
import androidx.room.Room

fun createRippleDatabase(context: Context): RippleDatabase = Room.databaseBuilder(
    context,
    RippleDatabase::class.java,
    "ripple.db"
).fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true).build()
