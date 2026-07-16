package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.Course
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class CourseApiImpl(
    private val client: HttpClient
): CourseApi {

    override suspend fun getCourses(): HttpResponse =
        client.get(Course())
}