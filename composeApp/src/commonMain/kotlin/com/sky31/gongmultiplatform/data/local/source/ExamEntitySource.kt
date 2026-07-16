package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.ExamEntity

interface ExamEntitySource {

    suspend fun insertExamEntity(entity: ExamEntity)

    suspend fun updateExamEntity(entity: ExamEntity)

    suspend fun getExamEntity(): ExamEntity?

    suspend fun deleteAllExamEntities()
}