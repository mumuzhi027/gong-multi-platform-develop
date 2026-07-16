package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.ClassroomData
import com.sky31.gongmultiplatform.util.NetworkResult

interface PublicRepository {

    suspend fun getTodayClassroom(): NetworkResult<ClassroomData>

    suspend fun getTomorrowClassroom(): NetworkResult<ClassroomData>

    suspend fun getCalendar(): NetworkResult<CalendarData>
}