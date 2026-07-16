package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.PublicDao
import com.sky31.gongmultiplatform.data.local.domain.PublicEntity
import com.sky31.gongmultiplatform.data.local.source.PublicEntitySourceImpl
import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.ClassroomData
import com.sky31.gongmultiplatform.util.decodeOrNull
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PublicDataRepositoryImpl(
    dao: PublicDao
): PublicDataRepository {

    private val source = PublicEntitySourceImpl(dao)
    private val writeMutex = Mutex()

    override suspend fun insertPublicData(
        todayClassroom: ClassroomData?,
        tomorrowClassroom: ClassroomData?,
        calendar: CalendarData?
    ) {
        val entity = PublicEntity(
            todayClassroom = todayClassroom?.let { Json.encodeToString<ClassroomData>(it) },
            tomorrowClassroom = tomorrowClassroom?.let { Json.encodeToString<ClassroomData>(it) },
            calendar = calendar?.let { Json.encodeToString<CalendarData>(it) }
        )

        source.insertPublicEntity(entity)
    }

    override suspend fun updatePublicData(
        todayClassroom: ClassroomData?,
        tomorrowClassroom: ClassroomData?,
        calendar: CalendarData?
    ) {
        writeMutex.withLock {
            val oldEntity = source.getPublicEntity()

            val entity = PublicEntity(
                todayClassroom = todayClassroom?.let { Json.encodeToString<ClassroomData>(it) } ?: oldEntity?.todayClassroom,
                tomorrowClassroom = tomorrowClassroom?.let { Json.encodeToString<ClassroomData>(it) } ?: oldEntity?.tomorrowClassroom,
                calendar = calendar?.let { Json.encodeToString<CalendarData>(it) } ?: oldEntity?.calendar
            )

            source.insertPublicEntity(entity)
        }
    }

    override suspend fun getCalendar(): CalendarData? {
        return decodeOrNull(
            raw = source.getCalendar(),
            label = "calendar"
        )
    }

    override suspend fun getTodayClassroom(): ClassroomData? {
        return decodeOrNull(
            raw = source.getTodayClassroom(),
            label = "today classroom"
        )
    }

    override suspend fun getTomorrowClassroom(): ClassroomData? {
        return decodeOrNull(
            raw = source.getTomorrowClassroom(),
            label = "tomorrow classroom"
        )
    }

    override suspend fun deleteAllPublicData() {
        source.deleteAllPublicEntities()
    }
}
