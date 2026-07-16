package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.dao.ExamDao
import com.sky31.gongmultiplatform.data.local.domain.ExamEntity

class ExamEntitySourceImpl(
    private val dao: ExamDao
): ExamEntitySource {

    override suspend fun insertExamEntity(entity: ExamEntity) {
        dao.insertExamEntity(entity)
    }

    override suspend fun updateExamEntity(entity: ExamEntity) {
        dao.updateExamEntity(entity)
    }

    override suspend fun getExamEntity(): ExamEntity? {
        return dao.getExamEntity()
    }

    override suspend fun deleteAllExamEntities() {
        dao.clearAll()
    }
}