package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.ClassroomData
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.retryExpiredDataState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ClassroomViewModel: ViewModel(), KoinComponent {
    companion object {
        private const val CLASSROOM_MAX_RETRY_TIMES = 10
        private const val CLASSROOM_RETRY_INTERVAL = 3000L
    }

    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val publicRepository: PublicRepositoryImpl by inject()

    private val _todayClassroomMap = MutableStateFlow<Map<String, List<ClassroomData.ClassroomInfo>>?>(null)
    val todayClassroomMap = _todayClassroomMap.asStateFlow()

    private val _tomorrowClassroomMap = MutableStateFlow<Map<String, List<ClassroomData.ClassroomInfo>>?>(null)
    val tomorrowClassroomMap = _tomorrowClassroomMap.asStateFlow()

    private val _todayDate = MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    val todayDate = _todayDate.asStateFlow()

    private val _todayClassroomState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val todayClassroomState = _todayClassroomState.asStateFlow()

    private val _tomorrowClassroomState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val tomorrowClassroomState = _tomorrowClassroomState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayClassroomFromLocal()
            getTomorrowClassroomFromLocal()

            if (!TokenState.isOfflineMode.value) {
                update()
            }
        }
    }

    suspend fun update() {
        _todayClassroomState.value = DataState.Loading
        _tomorrowClassroomState.value = DataState.Loading

        val results = coroutineScope {
            listOf(
                async { updateTodayClassroomWithRetry() },
                async { updateTomorrowClassroomWithRetry() }
            ).awaitAll()
        }

        _todayClassroomState.value = results[0]
        _tomorrowClassroomState.value = results[1]
    }

    private suspend fun getTodayClassroomFromLocal() {
        publicDataRepository.getTodayClassroom()?.let {
            _todayClassroomMap.value = it.classrooms
        }
    }

    suspend fun updateTodayClassroom(): DataState {
        when(val result = publicRepository.getTodayClassroom()) {
            is NetworkResult.Success -> {
                publicDataRepository.updatePublicData(
                    todayClassroom = result.data
                )

                getTodayClassroomFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateTodayClassroomWithRetry(): DataState {
        return retryExpiredDataState(
            maxRetryTimes = CLASSROOM_MAX_RETRY_TIMES,
            retryIntervalMillis = CLASSROOM_RETRY_INTERVAL,
            finalErrorMessage = "今日空教室生成较慢，请稍后下拉刷新"
        ) {
            updateTodayClassroom()
        }
    }

    private suspend fun getTomorrowClassroomFromLocal() {
        publicDataRepository.getTomorrowClassroom()?.let {
            _tomorrowClassroomMap.value = it.classrooms
        }
    }

    suspend fun updateTomorrowClassroom(): DataState {
        when(val result = publicRepository.getTomorrowClassroom()) {
            is NetworkResult.Success -> {
                publicDataRepository.updatePublicData(
                    tomorrowClassroom = result.data
                )

                getTomorrowClassroomFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error -> {
                return codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateTomorrowClassroomWithRetry(): DataState {
        return retryExpiredDataState(
            maxRetryTimes = CLASSROOM_MAX_RETRY_TIMES,
            retryIntervalMillis = CLASSROOM_RETRY_INTERVAL,
            finalErrorMessage = "明日空教室生成较慢，请稍后下拉刷新"
        ) {
            updateTomorrowClassroom()
        }
    }

    fun resetLoadingState() {
        if (_todayClassroomState.value is DataState.Loading) {
            _todayClassroomState.value = DataState.Uninitialized
        }
        if (_tomorrowClassroomState.value is DataState.Loading) {
            _tomorrowClassroomState.value = DataState.Uninitialized
        }
    }
}
