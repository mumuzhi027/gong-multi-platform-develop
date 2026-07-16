package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "publicEntity")
data class PublicEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "today_classroom") val todayClassroom: String? = null,
    @ColumnInfo(name = "tomorrow_classroom") val tomorrowClassroom: String? = null,
    @ColumnInfo(name = "calendar") val calendar: String? = null,
)
