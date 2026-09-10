package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.network.repository.CourseRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.Toast
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.getWeekNum
import com.sky31.gongmultiplatform.util.isInThisWeek
import com.sky31.gongmultiplatform.util.safeApiCallsSequential
import com.sky31.gongmultiplatform.util.scheduleCourseAlarm
import com.sky31.gongmultiplatform.util.toCourseMap
import com.sky31.gongmultiplatform.util.updateAppWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CourseViewModel: ViewModel(), KoinComponent {
    data class EditorState(val day: String, val week: Long, val startTime: Int, val course: CourseElem? = null)
    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val courseRepository: CourseRepositoryImpl by inject()

    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val publicRepository: PublicRepositoryImpl by inject()

    private val _courseMap = MutableStateFlow(mapOf<String, List<CourseElem>>())

    private val _sheetCourse = MutableStateFlow<CourseElem?>(null)
    val sheetCourse = _sheetCourse.asStateFlow()

    private val _sheetVisible = MutableStateFlow<Boolean>(false)
    val sheetVisible = _sheetVisible.asStateFlow()

    private val _editorState = MutableStateFlow<EditorState?>(null)
    val editorState = _editorState.asStateFlow()

    private val _calendar = MutableStateFlow<CalendarData?>(null)
    val calendar = _calendar.asStateFlow()

    private val _currentTime = MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    val currentTime = _currentTime.asStateFlow()

    private val _currentWeekNum = MutableStateFlow(0L)
    val currentWeekNum = _currentWeekNum.asStateFlow()

    private val _courseMapState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val courseMapState = _courseMapState.asStateFlow()

    init {
        viewModelScope.launch {
            getCalendarFromLocal()
            getCourseMapFromLocal()
            updateCurrentWeekNum()
        }
    }

    suspend fun update() {
        _courseMapState.value = DataState.Loading

        val results = safeApiCallsSequential(
            calls = listOf(
                { updateCalendar() },
                { updateCourseMap() }
            )
        )

//        if(checkResults(results)) {
//            scheduleCourseAlarm()
//        }

        _courseMapState.value = results[1]

        if (results[1] == DataState.Newest) {
            scheduleCourseAlarm()
        }

        runCatching {
            updateAppWidget()
        }.onFailure {
            println("Failed to trigger app widget update from CourseViewModel: ${it.message}")
        }
    }

    private fun updateCurrentWeekNum() {
        _calendar.value?.let {
            _currentWeekNum.value = getWeekNum(it, _currentTime.value)
        }
    }

    private suspend fun updateCalendar(): DataState {
        when(val result = publicRepository.getCalendar()) {
            is NetworkResult.Success -> {
                publicDataRepository.insertPublicData(calendar = result.data)

                getCalendarFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun getCalendarFromLocal() {
        publicDataRepository.getCalendar()?.let {
            _calendar.value = it
        }
    }

    private suspend fun updateCourseMap(): DataState {
        when(val result = courseRepository.getCourses()) {
            is NetworkResult.Success -> {
                courseDataRepository.replaceRemoteCoursesPreservingCustom(toCourseMap(result.data))

                getCourseMapFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun getCourseMapFromLocal() {
        courseDataRepository.getCourseMap()?.let { courseMap ->
            _courseMap.value = courseMap
        }
    }

    /**
     * 获取对应周次的课程表
     *
     * @param weekNum 周次
     */
    fun getWeekCourseMap(weekNum: Long): Flow<Map<String, List<CourseElem>>> {
        return _courseMap.map { courseMap ->
            val resultMap = mutableMapOf<String, List<CourseElem>>()

            courseMap.forEach { (weekday: String, courses: List<CourseElem>) ->
                val courseList = courses
                    .filter { isInThisWeek(weekNum, it) }
                    .sortedBy { it.startTime }

                resultMap[weekday] = courseList
            }

            resultMap
        }
    }

    fun showSheet() {
        _sheetVisible.value = true
    }

    fun hideSheet() {
        _sheetVisible.value = false
    }

    fun setSheetCourse(course: CourseElem) {
        _sheetCourse.value = course
    }

    fun openEditor(day: String, week: Long, startTime: Int, course: CourseElem? = null) {
        _editorState.value = EditorState(day, week, startTime, course)
    }

    fun closeEditor() { _editorState.value = null }

    fun saveCustomCourse(name: String, teacher: String, classroom: String, week: Long, start: Int, end: Int) {
        val editor = _editorState.value ?: return
        if (name.isBlank() || week < 1 || start !in 1..11 || end !in start..11) return
        viewModelScope.launch {
            editor.course?.let { courseDataRepository.deleteCourse(it) }
            val map = courseDataRepository.getCourseMap().orEmpty().mapValues { it.value.toMutableList() }.toMutableMap()
            val list = map.getOrPut(editor.day) { mutableListOf() }
            list += CourseElem(name.trim(), teacher.trim(), classroom.trim(), week.toString(), start, end - start + 1, editor.day, true)
            map[editor.day] = list.sortedBy { it.startTime }.toMutableList()
            courseDataRepository.updateCourseMap(map)
            getCourseMapFromLocal()
            updateAppWidget()
            _editorState.value = null
        }
    }

    fun deleteCourse() {
        val editor = _editorState.value ?: return
        val course = editor.course ?: return
        viewModelScope.launch {
            courseDataRepository.deleteCourse(course)
            getCourseMapFromLocal()
            updateAppWidget()
            _editorState.value = null
        }
    }

    fun deleteSheetCourse() {
        val course = _sheetCourse.value ?: return
        viewModelScope.launch {
            courseDataRepository.deleteCourse(course)
            getCourseMapFromLocal()
            updateAppWidget()
            _sheetVisible.value = false
            _sheetCourse.value = null
        }
    }

    fun restoreRemoteCoursesForWeek(week: Long) {
        if (week < 1) return
        viewModelScope.launch {
            val restoredCount = courseDataRepository.restoreRemoteCoursesForWeek(week)
            getCourseMapFromLocal()
            updateAppWidget()
            if (restoredCount > 0) {
                Toast.show("已恢复第${week}周原课表内容")
            } else {
                Toast.show("第${week}周没有需要恢复的课程")
            }
        }
    }
}
