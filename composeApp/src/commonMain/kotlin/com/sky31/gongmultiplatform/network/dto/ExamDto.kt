package com.sky31.gongmultiplatform.network.dto

import com.sky31.gongmultiplatform.model.ExamElem
import kotlinx.serialization.Serializable

@Serializable
data class ExamDto(
    val exams: List<ExamElem>
)
