package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.util.NetworkResult

interface AcademicRepository {

    suspend fun getTotalRank(forceRefresh: Boolean = false): NetworkResult<RankData>

    suspend fun getCompulsoryRank(forceRefresh: Boolean = false): NetworkResult<RankData>

    suspend fun getMajorAcademicInfo(forceRefresh: Boolean = false): NetworkResult<ScoreData>

    suspend fun getMinorAcademicInfo(forceRefresh: Boolean = false): NetworkResult<ScoreData>
}
