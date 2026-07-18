package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RankData(
    // 平均成绩
    @SerialName("average_score") val averageScore: String,
    // 绩点
    val gpa: String,
    // 班级排名
    @SerialName("class_rank") val classRank: Int,
    // 专业排名
    @SerialName("major_rank") val majorRank: Int,
    // 学期
    val terms: List<String>,
    // 加权平均成绩排名
    @SerialName("weighted_average_rank") val weightedAverageRank: Int = 0
)
