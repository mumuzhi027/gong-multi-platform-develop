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
        // Older builds stored the complete remote week expression (for example
        // 1-16) when deleting one occurrence. Those tombstones accidentally hid
        // every occurrence after refresh. Discard them so affected users recover
        // the original course automatically.
        val deletedForSingleWeeks = stored.deletedRemoteCourses.filter { it.activeWeeks().size == 1 }
        val custom = stored.courses.mapValues { (_, items) -> items.filter { it.isCustom } }
        val mergedKeys = courses.keys + custom.keys
        val merged = mergedKeys.associateWith { day ->
            (courses[day].orEmpty() + custom[day].orEmpty()).sortedBy { it.startTime }
        }
        writeStoredData(stored.copy(courses = merged, deletedRemoteCourses = deletedForSingleWeeks))
    }

    override suspend fun updateCourseMap(courses: Map<String, List<CourseElem>>) {
        val stored = getStoredData()
        // This method is used when custom courses are added or edited. Keep the
        // untouched remote data in storage; `courses` is the visible projection
        // and may already have one or more deleted occurrences filtered out.
        val remote = stored.courses.mapValues { (_, items) -> items.filterNot { it.isCustom } }
        val custom = courses.mapValues { (_, items) -> items.filter { it.isCustom } }
        val mergedKeys = remote.keys + custom.keys
        val merged = mergedKeys.associateWith { day ->
            (remote[day].orEmpty() + custom[day].orEmpty()).sortedBy { it.startTime }
        }
        writeStoredData(stored.copy(courses = merged))
    }

    override suspend fun getCourseMap(): Map<String, List<CourseElem>>? {
        return source.getCourseEntity()?.let {
            val stored = getStoredData()
            applyDeletedOccurrences(stored).mapValues { (_, courses) ->
                courses.filter { course ->
                    val valid = course.isValidForSchedule()
                    if (!valid) println("Skipped invalid cached course: $course")
                    valid
                }
            }
        }
    }

    override suspend fun deleteCourse(course: CourseElem, week: Long) {
        if (week < 1 || !isInThisWeek(week, course)) return
        val stored = getStoredData()
        val courses = if (course.isCustom) {
            stored.courses.mapValues { (_, items) ->
                items.flatMap { item ->
                    if (item == course) item.withoutWeeks(setOf(week))?.let(::listOf).orEmpty()
                    else listOf(item)
                }
            }
        } else stored.courses
        val alreadyDeleted = stored.deletedRemoteCourses.any {
            it.matchesRemoteIgnoringWeeks(course) && isInThisWeek(week, it)
        }
        val deletedRemote = if (course.isCustom || alreadyDeleted) {
            stored.deletedRemoteCourses
        } else {
            stored.deletedRemoteCourses + course.copy(weeks = week.toString(), isCustom = false)
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
                val existingIndex = dayCourses.indexOfFirst { it.matchesRemoteIgnoringWeeks(remote) }
                if (existingIndex >= 0) {
                    val existing = dayCourses[existingIndex]
                    dayCourses[existingIndex] = existing.copy(
                        weeks = (existing.activeWeeks() + remote.activeWeeks()).sorted().joinToString(",")
                    )
                } else {
                    dayCourses += remote
                }
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

    /**
     * Build the visible course map without mutating the remotely fetched course
     * definitions. A deletion is always an occurrence identified by course and
     * week, never deletion of the complete repeating course.
     */
    private fun applyDeletedOccurrences(data: StoredCourseData): Map<String, List<CourseElem>> {
        val singleWeekDeletions = data.deletedRemoteCourses.filter { it.activeWeeks().size == 1 }
        val legacyWholeCourseDeletions = data.deletedRemoteCourses.filter { it.activeWeeks().size != 1 }
        val recoveredCourses = data.courses.mapValues { it.value.toMutableList() }.toMutableMap()

        // Old builds removed the remote course from `courses` and retained only
        // its full-range tombstone. Recover it locally even while offline; the
        // next successful refresh permanently migrates the stored structure.
        legacyWholeCourseDeletions.forEach { deleted ->
            val dayCourses = recoveredCourses.getOrPut(deleted.day) { mutableListOf() }
            if (dayCourses.none { it.matchesRemote(deleted) }) {
                dayCourses += deleted.copy(isCustom = false)
            }
        }

        return recoveredCourses.mapValues { (_, items) ->
            items.mapNotNull { course ->
                if (course.isCustom) return@mapNotNull course
                val deletedWeeks = singleWeekDeletions
                    .filter { it.matchesRemoteIgnoringWeeks(course) }
                    .flatMapTo(mutableSetOf()) { it.activeWeeks() }
                course.withoutWeeks(deletedWeeks)
            }
        }
    }

    private fun CourseElem.matchesRemote(other: CourseElem): Boolean =
        matchesRemoteIgnoringWeeks(other) && weeks == other.weeks

    private fun CourseElem.matchesRemoteIgnoringWeeks(other: CourseElem): Boolean =
        name == other.name &&
            teacher == other.teacher &&
            classroom == other.classroom &&
            startTime == other.startTime &&
            duration == other.duration &&
            day == other.day

    private fun CourseElem.activeWeeks(): Set<Long> =
        (1L..60L).filterTo(linkedSetOf()) { week ->
            runCatching { isInThisWeek(week, this) }.getOrDefault(false)
        }

    private fun CourseElem.withoutWeeks(removedWeeks: Set<Long>): CourseElem? {
        if (removedWeeks.isEmpty()) return this
        val remaining = activeWeeks() - removedWeeks
        return if (remaining.isEmpty()) null else copy(weeks = remaining.sorted().joinToString(","))
    }

    private fun CourseElem.overlaps(other: CourseElem): Boolean {
        val end = startTime + duration - 1
        val otherEnd = other.startTime + other.duration - 1
        return startTime <= otherEnd && other.startTime <= end
    }
}
