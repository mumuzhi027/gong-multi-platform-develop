package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academicEntity")
data class AcademicEntity(
    @PrimaryKey val uid: Int? = 0,
    // 所有成绩排名
    @ColumnInfo(name = "total_rank") val totalRank: String? = null,
    // 必修成绩排名
    @ColumnInfo(name = "compulsory_rank") val compulsoryRank: String? = null,
    // 主修成绩
    @ColumnInfo(name = "major_score") val majorScore: String? = null,
    // 辅修成绩
    @ColumnInfo(name = "minor_score") val minorScore: String? = null,
)
