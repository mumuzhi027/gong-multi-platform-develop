package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface ExamApi {
    suspend fun getExamList(): HttpResponse
}