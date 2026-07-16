package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface AcademicApi {

    suspend fun getMajorAcademicInfo(): HttpResponse

    suspend fun getMinorAcademicInfo(): HttpResponse

    suspend fun getTotalRank(): HttpResponse

    suspend fun getCompulsoryRank(): HttpResponse
}