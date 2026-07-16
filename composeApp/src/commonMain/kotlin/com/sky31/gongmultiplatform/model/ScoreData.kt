package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreData(
    @SerialName("student_id") val studentId: String,
    val name: String,
    val college: String,
    val major: String,
    val scores: List<ScoreElem>,
    @SerialName("total_credit") val totalCredit: List<String>,
    @SerialName("elective_credit") val electiveCredit: List<String>,
    @SerialName("compulsory_credit") val compulsoryCredit: List<String>,
    @SerialName("cross_course_credit") val crossCourseCredit: List<String>,
    @SerialName("average_score") val averageScore: String,
    val gpa: String,
    val cet4: String,
    val cet6: String
) {
    @Serializable
    data class ScoreElem(
        val name: String,
        val score: String,
        val credit: String,
        val type: String,
        val term: Int
    )
}

