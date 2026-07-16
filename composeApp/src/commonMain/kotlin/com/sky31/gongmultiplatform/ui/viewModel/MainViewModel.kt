package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ExamDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.model.config.NotificationConfig
import com.sky31.gongmultiplatform.network.repository.CourseRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.ExamRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.getCourseList
import com.sky31.gongmultiplatform.util.safeApiCallsSequential
import com.sky31.gongmultiplatform.util.toCourseMap
import com.sky31.gongmultiplatform.util.updateAppWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainViewModel: ViewModel(), KoinComponent {

    private val courseRepository: CourseRepositoryImpl by inject()
    private val examRepository: ExamRepositoryImpl by inject()
    private val publicRepository: PublicRepositoryImpl by inject()

    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val examDataRepository: ExamDataRepositoryImpl by inject()
    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val configRepository: ConfigRepositoryImpl by inject()

    private val _notificationConfig = MutableStateFlow(NotificationConfig())

    private val _currentTime = MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    val currentTime = _currentTime.asStateFlow()

    private val _courseList = MutableStateFlow<List<CourseElem>>(emptyList())
    val courseList = _courseList.asStateFlow()

    private val _completedCourseNum = MutableStateFlow(0)
    val completedCourseNum = _completedCourseNum.asStateFlow()

    private val _examList = MutableStateFlow<List<ExamElem>>(emptyList())
    val examList = _examList.asStateFlow()

    private val _calendar = MutableStateFlow<CalendarData?>(null)
    val calendar = _calendar.asStateFlow()

    private val _progression = MutableStateFlow(-1f)
    val progression = _progression.asStateFlow()

    private val _examBoxState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val examBoxState = _examBoxState.asStateFlow()

    private val _courseBoxState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val courseBoxState = _courseBoxState.asStateFlow()

    init {
        viewModelScope.launch {
            getCalendarFromLocal()
            getCourseListFromLocal()
            getExamListFromLocal()
            getNotificationConfigFromLocal()
        }
    }

    fun refreshCurrentTime() {
        _currentTime.value = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    suspend fun updateExamBox() {
        _examBoxState.value = DataState.Loading

        val results = safeApiCallsSequential(
            calls = listOf(
                { updateCalendar() },
                { updateExamList() }
            )
        )

//        if(checkResults(results) && _notificationConfig.value.examNotification) {
//            scheduleExamAlarm(_examList.value)
//        }

        _examBoxState.value = results[1]
    }

    suspend fun updateCourseBox() {
        _courseBoxState.value = DataState.Loading

        val results = safeApiCallsSequential(
            calls = listOf(
                { updateCalendar() },
                { updateCourseList() }
            )
        )

//        if(checkResults(results) && _notificationConfig.value.courseNotification) {
//            scheduleCourseAlarm()
//        }

        _courseBoxState.value = results[1]

        runCatching {
            updateAppWidget()
        }.onFailure {
            println("Failed to trigger app widget update from MainViewModel: ${it.message}")
        }
    }

    suspend fun updateCourseList(): DataState {
        when(val result = courseRepository.getCourses()) {
            is NetworkResult.Success -> {
                courseDataRepository.insertCourseMap(toCourseMap(result.data))

                getCourseListFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun getCourseListFromLocal() {
        courseDataRepository.getCourseMap()?.let { courseMap ->
            calendar.value?.let { calendar ->
                _courseList.value = getCourseList(courseMap, calendar, currentTime.value)
            }
        }
    }

    suspend fun updateExamList(): DataState {
        when(val result = examRepository.getExamList()) {
            is NetworkResult.Success -> {
                examDataRepository.insertExamData(result.data)

                getExamListFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun getExamListFromLocal() {
        examDataRepository.getExamList()?.let {
            _examList.value = it
        }
    }

    suspend fun updateCalendar(): DataState {
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

    suspend fun getCalendarFromLocal() {
        publicDataRepository.getCalendar()?.let {
            _calendar.value = it
        }
    }

    suspend fun getNotificationConfigFromLocal() {
        configRepository.getNotificationConfig()?.let {
            _notificationConfig.value = it
        }
    }

    fun setProgression(progression: Float) {
        _progression.value = progression
    }

    fun setCompletedNum(num: Int) {
        _completedCourseNum.value = num
    }

    fun resetExamBoxState() {
        _examBoxState.value = DataState.Uninitialized
    }

    fun resetCourseBoxState() {
        _courseBoxState.value = DataState.Uninitialized
    }
}
