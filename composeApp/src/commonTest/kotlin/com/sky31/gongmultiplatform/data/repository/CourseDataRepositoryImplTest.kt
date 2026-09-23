package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.isInThisWeek
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseDataRepositoryImplTest {

    @Test
    fun deletingOneOccurrenceKeepsTheOtherWeeksAfterRefresh() = runTest {
        val repository = CourseDataRepositoryImpl(FakeCourseDao())
        val remote = remoteCourse()
        val remoteMap = mapOf("Friday" to listOf(remote))

        repository.insertCourseMap(remoteMap)
        repository.deleteCourse(remote, week = 4)
        repository.replaceRemoteCoursesPreservingCustom(remoteMap)

        val course = repository.getCourseMap().orEmpty().getValue("Friday").single()
        assertFalse(isInThisWeek(4, course))
        assertTrue(isInThisWeek(5, course))
        assertTrue(isInThisWeek(16, course))
    }

    @Test
    fun refreshDropsLegacyWholeCourseTombstones() = runTest {
        val remote = remoteCourse()
        val encodedCourse = Json.encodeToString(remote)
        val legacyData = """{"courses":{"Friday":[]},"deletedRemoteCourses":[$encodedCourse]}"""
        val repository = CourseDataRepositoryImpl(
            FakeCourseDao(CourseEntity(courses = legacyData))
        )

        val recoveredOffline = repository.getCourseMap().orEmpty().getValue("Friday").single()
        assertTrue(isInThisWeek(4, recoveredOffline))
        assertTrue(isInThisWeek(16, recoveredOffline))

        repository.replaceRemoteCoursesPreservingCustom(mapOf("Friday" to listOf(remote)))

        val recovered = repository.getCourseMap().orEmpty().getValue("Friday").single()
        assertTrue(isInThisWeek(4, recovered))
        assertTrue(isInThisWeek(16, recovered))
    }

    @Test
    fun deletingOneOccurrenceWorksForCoursesSplitIntoSeveralWeekRecords() = runTest {
        val repository = CourseDataRepositoryImpl(FakeCourseDao())
        val firstHalf = remoteCourse().copy(weeks = "1-8")
        val secondHalf = remoteCourse().copy(weeks = "9-16")
        val remoteMap = mapOf("Friday" to listOf(firstHalf, secondHalf))

        repository.insertCourseMap(remoteMap)
        repository.deleteCourse(firstHalf, week = 4)

        val courses = repository.getCourseMap().orEmpty().getValue("Friday")
        assertFalse(courses.any { isInThisWeek(4, it) })
        assertTrue(courses.any { isInThisWeek(5, it) })
        assertTrue(courses.any { isInThisWeek(12, it) })
    }

    @Test
    fun addingCustomCourseDoesNotOverwriteRawRemoteWeeks() = runTest {
        val repository = CourseDataRepositoryImpl(FakeCourseDao())
        val remote = remoteCourse()
        repository.insertCourseMap(mapOf("Friday" to listOf(remote)))
        repository.deleteCourse(remote, week = 4)

        val visible = repository.getCourseMap().orEmpty().mapValues { it.value.toMutableList() }.toMutableMap()
        visible.getValue("Friday") += remote.copy(
            name = "自习",
            weeks = "4",
            isCustom = true
        )
        repository.updateCourseMap(visible)
        repository.restoreRemoteCoursesForWeek(week = 4)

        val courses = repository.getCourseMap().orEmpty().getValue("Friday")
        assertTrue(courses.any { !it.isCustom && isInThisWeek(4, it) })
        assertFalse(courses.any { it.isCustom })
    }

    private fun remoteCourse() = CourseElem(
        name = "测试课程",
        teacher = "测试教师",
        classroom = "测试教室",
        weeks = "1-16",
        startTime = 1,
        duration = 2,
        day = "Friday"
    )

    private class FakeCourseDao(
        private var entity: CourseEntity? = null
    ) : CourseDao {
        override suspend fun getCourseEntity(): CourseEntity? = entity

        override suspend fun updateCourseEntity(entity: CourseEntity) {
            this.entity = entity
        }

        override suspend fun insertCourseEntity(data: CourseEntity) {
            entity = data
        }

        override suspend fun clearAll() {
            entity = null
        }
    }
}
