package com.sky31.gongmultiplatform.network.dto

import com.sky31.gongmultiplatform.model.CourseElem
import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val courses: List<CourseElem>
)
