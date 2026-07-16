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
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.getWeekNum
import com.sky31.gongmultiplatform.util.isInThisWeek
import com.sky31.gongmultiplatform.util.safeApiCallsSequential
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
    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val courseRepository: CourseRepositoryImpl by inject()

    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val publicRepository: PublicRepositoryImpl by inject()

    private val _courseMap = MutableStateFlow(mapOf<String, List<CourseElem>>())

    private val _sheetCourse = MutableStateFlow<CourseElem?>(null)
    val sheetCourse = _sheetCourse.asStateFlow()

    private val _sheetVisible = MutableStateFlow<Boolean>(false)
    val sheetVisible = _sheetVisible.asStateFlow()

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
                courseDataRepository.insertCourseMap(toCourseMap(result.data))

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
}
