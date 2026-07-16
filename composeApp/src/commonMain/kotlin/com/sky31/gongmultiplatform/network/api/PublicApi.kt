package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface PublicApi {

    suspend fun getTodayClassroom(): HttpResponse

    suspend fun getTomorrowClassroom(): HttpResponse

    suspend fun getCalendar(): HttpResponse
}