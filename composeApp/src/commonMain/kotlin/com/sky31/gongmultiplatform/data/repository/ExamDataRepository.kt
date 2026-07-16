package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.ExamElem

interface ExamDataRepository {

    suspend fun insertExamData(exams: List<ExamElem>)

    suspend fun updateExamData(exams: List<ExamElem>)

    suspend fun getExamList(): List<ExamElem>?

    suspend fun deleteAllExams()
}