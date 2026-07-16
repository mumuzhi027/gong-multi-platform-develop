package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courseEntity")
data class CourseEntity(
    @PrimaryKey val uid: Int = 1,
    val courses: String? = null,
)
