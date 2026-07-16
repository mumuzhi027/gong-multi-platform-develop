package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.ClassroomData

interface PublicDataRepository {

    suspend fun insertPublicData(
        todayClassroom: ClassroomData? = null,
        tomorrowClassroom: ClassroomData? = null,
        calendar: CalendarData? = null
    )

    suspend fun updatePublicData(
        todayClassroom: ClassroomData? = null,
        tomorrowClassroom: ClassroomData? = null,
        calendar: CalendarData? = null
    )

    suspend fun getTodayClassroom(): ClassroomData?

    suspend fun getTomorrowClassroom(): ClassroomData?

    suspend fun getCalendar(): CalendarData?

    suspend fun deleteAllPublicData()
}