package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examEntity")
data class ExamEntity(
    @PrimaryKey val id: Int = 1,
    val exams: String? = null,
)
