package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface AcademicApi {

    suspend fun getMajorAcademicInfo(forceRefresh: Boolean = false): HttpResponse

    suspend fun getMinorAcademicInfo(forceRefresh: Boolean = false): HttpResponse

    suspend fun getTotalRank(forceRefresh: Boolean = false): HttpResponse

    suspend fun getCompulsoryRank(forceRefresh: Boolean = false): HttpResponse
}
