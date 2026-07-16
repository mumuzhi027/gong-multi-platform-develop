package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.ExamDao
import com.sky31.gongmultiplatform.data.local.domain.ExamEntity
import com.sky31.gongmultiplatform.data.local.source.ExamEntitySourceImpl
import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.util.decodeOrNull
import kotlinx.serialization.json.Json

class ExamDataRepositoryImpl(
    dao: ExamDao
): ExamDataRepository {

    private val source = ExamEntitySourceImpl(dao)

    override suspend fun insertExamData(exams: List<ExamElem>) {
        source.insertExamEntity(ExamEntity(
            exams = Json.encodeToString<List<ExamElem>>(exams)
        ))
    }

    override suspend fun updateExamData(exams: List<ExamElem>) {
        source.updateExamEntity(ExamEntity(
            exams = Json.encodeToString<List<ExamElem>>(exams)
        ))
    }

    override suspend fun getExamList(): List<ExamElem>? {
        val entity = source.getExamEntity()

        return decodeOrNull(
            raw = entity?.exams,
            label = "exam list"
        )
    }

    override suspend fun deleteAllExams() {
        source.deleteAllExamEntities()
    }
}
