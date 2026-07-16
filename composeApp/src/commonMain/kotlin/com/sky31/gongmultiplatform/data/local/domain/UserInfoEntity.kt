package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userInfoEntity")
data class UserInfoEntity(
    @PrimaryKey
    @ColumnInfo(name = "student_id") val studentID: String = "",
    val name: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val major: String? = null,
    @ColumnInfo(name = "class_") val clazz: String? = null,
    @ColumnInfo(name = "entrance_day") val entranceDay: String? = null
)
