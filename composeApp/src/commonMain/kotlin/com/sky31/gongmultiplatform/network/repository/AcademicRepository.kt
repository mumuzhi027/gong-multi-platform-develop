package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.util.NetworkResult

interface AcademicRepository {

    suspend fun getTotalRank(): NetworkResult<RankData>

    suspend fun getCompulsoryRank(): NetworkResult<RankData>

    suspend fun getMajorAcademicInfo(): NetworkResult<ScoreData>

    suspend fun getMinorAcademicInfo(): NetworkResult<ScoreData>
}