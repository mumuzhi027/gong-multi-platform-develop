package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.util.NetworkResult

interface ExamRepository {

    suspend fun getExamList(): NetworkResult<List<ExamElem>>
}