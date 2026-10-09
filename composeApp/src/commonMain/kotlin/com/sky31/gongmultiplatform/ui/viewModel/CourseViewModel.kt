package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.network.repository.CourseRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.COURSE_TIME_GROUP_MASK
import com.sky31.gongmultiplatform.util.MAX_COURSE_WEEK
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.Toast
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.createEncryptedSettings
import com.sky31.gongmultiplatform.util.formatWeeksRange
import com.sky31.gongmultiplatform.util.getWeekNum
import com.sky31.gongmultiplatform.util.visibleCoursesForWeek
import com.sky31.gongmultiplatform.util.safeApiCallsSequential
import com.sky31.gongmultiplatform.util.scheduleCourseAlarm
import com.sky31.gongmultiplatform.util.toCourseMap
import com.sky31.gongmultiplatform.util.updateAppWidget
import com.sky31.gongmultiplatform.util.weekdayNameMap
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

    private val settingsFactory: Settings.Factory by inject()
    private val timeAxisSettings: Settings by lazy {
        createEncryptedSettings(settingsFactory, name = "course_time_axis")
    }
    // 同步读取本地设置，首次打开就显示上次选择，避免先闪回常规模式。
    private val _staggeredTimeGroups = MutableStateFlow(runCatching {
        timeAxisSettings.getInt("staggered_groups", defaultValue = 0)
            .takeIf { it in 0..COURSE_TIME_GROUP_MASK } ?: 0
    }.getOrElse {
        println("Failed to read course time axis preference: ${it.message}")
        0
    })
    val staggeredTimeGroups = _staggeredTimeGroups.asStateFlow()

    fun toggleTimeGroup(group: Int) {
        if (group !in 0..5) return
        saveTimeGroups(_staggeredTimeGroups.value xor (1 shl group))
    }

    fun toggleAllTimes() {
        saveTimeGroups(if (_staggeredTimeGroups.value == COURSE_TIME_GROUP_MASK) 0 else COURSE_TIME_GROUP_MASK)
    }

    private fun saveTimeGroups(groups: Int) {
        val value = groups and COURSE_TIME_GROUP_MASK
        _staggeredTimeGroups.value = value
        // 点击时立即写入独立的本地偏好，离开页面不依赖协程或页面销毁回调。
        runCatching { timeAxisSettings.putInt("staggered_groups", value) }.onFailure {
            println("Failed to save course time axis preference: ${it.message}")
        }
    }

    private val _courseMap = MutableStateFlow(mapOf<String, List<CourseElem>>())

    private val _sheetCourse = MutableStateFlow<CourseElem?>(null)
    val sheetCourse = _sheetCourse.asStateFlow()
    private var sheetWeek: Long = 0

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
            if (!TokenState.isOfflineMode.value && (_calendar.value == null || _courseMap.value.isEmpty())) {
                update()
            }
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
                updateCurrentWeekNum()

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

            weekdayNameMap.values.forEach { weekday ->
                val courseList = visibleCoursesForWeek(weekNum, courseMap[weekday].orEmpty())

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

    fun setSheetCourse(course: CourseElem, week: Long) {
        _sheetCourse.value = course
        sheetWeek = week
    }

    fun openEditor(day: String, week: Long, startTime: Int, course: CourseElem? = null) {
        _editorState.value = EditorState(day, week, startTime, course)
    }

    fun closeEditor() { _editorState.value = null }

    fun saveCustomCourse(name: String, teacher: String, classroom: String, startWeek: Long, endWeek: Long, start: Int, end: Int) {
        val editor = _editorState.value ?: return
        if (name.isBlank() ||
            startWeek !in 1..MAX_COURSE_WEEK || endWeek !in startWeek..MAX_COURSE_WEEK ||
            start !in 1..11 || end !in start..11
        ) return
        viewModelScope.launch {
            val map = courseDataRepository.getCourseMap().orEmpty()
                .mapValues { (_, courses) -> courses.toMutableList() }
                .toMutableMap()
            // 编辑已有课程时移除原课程的全部周次，避免多周次课程产生重复条目
            editor.course?.let { origin -> map.values.forEach { it.remove(origin) } }
            val list = map.getOrPut(editor.day) { mutableListOf() }
            list += CourseElem(
                name.trim(),
                teacher.trim(),
                classroom.trim(),
                formatWeeksRange(startWeek, endWeek),
                start,
                end - start + 1,
                editor.day,
                true
            )
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
            val map = courseDataRepository.getCourseMap().orEmpty()
                .mapValues { (_, courses) -> courses.toMutableList() }
                .toMutableMap()
            // 自定义课程以整门课程为单位删除，避免多周次课程残留在其它周次
            map.values.forEach { it.remove(course) }
            courseDataRepository.updateCourseMap(map)
            getCourseMapFromLocal()
            updateAppWidget()
            _editorState.value = null
        }
    }

    fun deleteSheetCourse() {
        val course = _sheetCourse.value ?: return
        viewModelScope.launch {
            courseDataRepository.deleteCourse(course, sheetWeek)
            getCourseMapFromLocal()
            updateAppWidget()
            _sheetVisible.value = false
            _sheetCourse.value = null
            sheetWeek = 0
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
