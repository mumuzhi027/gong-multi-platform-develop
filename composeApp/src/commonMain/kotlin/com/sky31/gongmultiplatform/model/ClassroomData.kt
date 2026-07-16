package com.sky31.gongmultiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class ClassroomData(
    val date: String? = null,
    val classrooms: Map<String, List<ClassroomInfo>>? = null
) {
    @Serializable
    data class ClassroomInfo(
        val name: String,
        val status: List<String>
    )
}
