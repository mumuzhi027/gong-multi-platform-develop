package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity
import com.sky31.gongmultiplatform.data.local.source.CourseEntitySourceImpl
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.decodeOrNull
import com.sky31.gongmultiplatform.util.isInThisWeek
import com.sky31.gongmultiplatform.util.isValidForSchedule
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
private data class StoredCourseData(
    val courses: Map<String, List<CourseElem>> = emptyMap(),
    val deletedRemoteCourses: List<CourseElem> = emptyList()
)

class CourseDataRepositoryImpl(
    dao: CourseDao
): CourseDataRepository {

    private val source = CourseEntitySourceImpl(dao)

    override suspend fun insertCourseMap(courses: Map<String, List<CourseElem>>) {
        writeStoredData(getStoredData().copy(courses = courses))
    }

    override suspend fun replaceRemoteCoursesPreservingCustom(courses: Map<String, List<CourseElem>>) {
        val stored = getStoredData()
        val custom = stored.courses.mapValues { (_, items) -> items.filter { it.isCustom } }
        val visibleRemote = courses.mapValues { (_, items) ->
            items.filterNot { remote -> stored.deletedRemoteCourses.any { it.matchesRemote(remote) } }
        }
        val mergedKeys = courses.keys + custom.keys
        val merged = mergedKeys.associateWith { day ->
            (visibleRemote[day].orEmpty() + custom[day].orEmpty()).sortedBy { it.startTime }
        }
        writeStoredData(stored.copy(courses = merged))
    }

    override suspend fun updateCourseMap(courses: Map<String, List<CourseElem>>) {
        writeStoredData(getStoredData().copy(courses = courses))
    }

    override suspend fun getCourseMap(): Map<String, List<CourseElem>>? {
        return source.getCourseEntity()?.let {
            getStoredData().courses.mapValues { (_, courses) ->
                courses.filter { course ->
                    val valid = course.isValidForSchedule()
                    if (!valid) println("Skipped invalid cached course: $course")
                    valid
                }
            }
        }
    }

    override suspend fun deleteCourse(course: CourseElem) {
        val stored = getStoredData()
        val courses = stored.courses.mapValues { (_, items) ->
            items.filterNot { it == course }
        }
        val deletedRemote = if (course.isCustom || stored.deletedRemoteCourses.any { it.matchesRemote(course) }) {
            stored.deletedRemoteCourses
        } else {
            stored.deletedRemoteCourses + course.copy(isCustom = false)
        }
        writeStoredData(StoredCourseData(courses, deletedRemote))
    }

    override suspend fun restoreRemoteCoursesForWeek(week: Long): Int {
        val stored = getStoredData()
        val restored = stored.deletedRemoteCourses.filter { course ->
            runCatching { isInThisWeek(week, course) }.getOrDefault(false)
        }
        if (restored.isEmpty()) return 0

        val courses = stored.courses.mapValues { it.value.toMutableList() }.toMutableMap()
        restored.groupBy { it.day }.forEach { (day, remoteCourses) ->
            val dayCourses = courses.getOrPut(day) { mutableListOf() }
            dayCourses.removeAll { custom ->
                custom.isCustom &&
                    runCatching { isInThisWeek(week, custom) }.getOrDefault(false) &&
                    remoteCourses.any { remote -> custom.overlaps(remote) }
            }
            remoteCourses.forEach { remote ->
                if (dayCourses.none { it.matchesRemote(remote) }) dayCourses += remote
            }
            courses[day] = dayCourses.sortedBy { it.startTime }.toMutableList()
        }

        val remainingDeleted = stored.deletedRemoteCourses.filterNot { deleted ->
            restored.any { it.matchesRemote(deleted) }
        }
        writeStoredData(StoredCourseData(courses, remainingDeleted))
        return restored.size
    }

    override suspend fun deleteAllCourses() {
        source.deleteAllCourseEntities()
    }

    private suspend fun getStoredData(): StoredCourseData {
        val raw = source.getCourseEntity()?.courses ?: return StoredCourseData()
        return runCatching { Json.decodeFromString<StoredCourseData>(raw) }.getOrElse {
            StoredCourseData(
                courses = decodeOrNull<Map<String, List<CourseElem>>>(raw, "course map").orEmpty()
            )
        }
    }

    private suspend fun writeStoredData(data: StoredCourseData) {
        source.insertCourseEntity(CourseEntity(courses = Json.encodeToString(data)))
    }

    private fun CourseElem.matchesRemote(other: CourseElem): Boolean =
        name == other.name &&
            teacher == other.teacher &&
            classroom == other.classroom &&
            weeks == other.weeks &&
            startTime == other.startTime &&
            duration == other.duration &&
            day == other.day

    private fun CourseElem.overlaps(other: CourseElem): Boolean {
        val end = startTime + duration - 1
        val otherEnd = other.startTime + other.duration - 1
        return startTime <= otherEnd && other.startTime <= end
    }
}
