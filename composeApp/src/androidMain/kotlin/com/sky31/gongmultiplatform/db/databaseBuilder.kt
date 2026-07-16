package com.sky31.gongmultiplatform.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sky31.gongmultiplatform.data.local.AppDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("gong_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}