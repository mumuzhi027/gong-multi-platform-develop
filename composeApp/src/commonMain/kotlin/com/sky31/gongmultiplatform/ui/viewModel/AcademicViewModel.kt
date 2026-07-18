package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.AcademicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.network.repository.AcademicRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.retryExpiredDataState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AcademicViewModel : ViewModel(), KoinComponent {
    companion object {
        private const val SCORE_MAX_RETRY_TIMES = 10
        private const val SCORE_RETRY_INTERVAL = 3000L
        private const val TOTAL_RANK_MAX_RETRY_TIMES = 10
        private const val TOTAL_RANK_RETRY_INTERVAL = 3000L
        private const val COMPULSORY_RANK_MAX_RETRY_TIMES = 10
        private const val COMPULSORY_RANK_RETRY_INTERVAL = 3000L
    }

    private val academicDataRepository: AcademicDataRepositoryImpl by inject()
    private val academicRepository: AcademicRepositoryImpl by inject()

    private val _majorScore = MutableStateFlow<ScoreData?>(null)
    val majorScore = _majorScore.asStateFlow()

    private val _minorScore = MutableStateFlow<ScoreData?>(null)
    val minorScore = _minorScore.asStateFlow()

    private val _compulsoryRank = MutableStateFlow<RankData?>(null)
    val compulsoryRank = _compulsoryRank.asStateFlow()

    private val _totalRank = MutableStateFlow<RankData?>(null)
    val totalRank = _totalRank.asStateFlow()

    private val _majorAcademicInfoState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val majorAcademicInfoState = _majorAcademicInfoState.asStateFlow()

    private val _minorAcademicInfoState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val minorAcademicInfoState = _minorAcademicInfoState.asStateFlow()

    private val _compulsoryRankState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val compulsoryRankState = _compulsoryRankState.asStateFlow()

    private val _totalRankState = MutableStateFlow<DataState>(DataState.Uninitialized)
    val totalRankState = _totalRankState.asStateFlow()

    init {
        viewModelScope.launch {
            getMajorScoreFromLocal()
            getMinorScoreFromLocal()
            getCompulsoryRankFromLocal()
            getTotalRankFromLocal()
        }
    }

    suspend fun update(forceRefresh: Boolean = false) {
        _minorAcademicInfoState.value = DataState.Loading
        _majorAcademicInfoState.value = DataState.Loading
        _compulsoryRankState.value = DataState.Loading
        _totalRankState.value = DataState.Loading

        val results = coroutineScope {
            listOf(
                async { updateMajorAcademicInfoWithRetry(forceRefresh) },
                async { updateMinorAcademicInfoWithRetry(forceRefresh) },
                async { updateCompulsoryRankWithRetry(forceRefresh) },
                async { updateTotalRankWithRetry(forceRefresh) }
            ).awaitAll()
        }

        _majorAcademicInfoState.value = results[0]
        _minorAcademicInfoState.value = results[1]
        _compulsoryRankState.value = results[2]
        _totalRankState.value = results[3]
    }

    private suspend fun getMajorScoreFromLocal() {
        academicDataRepository.getMajorScore()?.let {
            _majorScore.value = it
        }
    }

    suspend fun updateMajorAcademicInfo(forceRefresh: Boolean = false): DataState {
        return when (val result = academicRepository.getMajorAcademicInfo(forceRefresh)) {
            is NetworkResult.Success -> {
                academicDataRepository.updateAcademicData(
                    majorScore = result.data
                )
                getMajorScoreFromLocal()
                DataState.Newest
            }

            is NetworkResult.Error -> {
                codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateMajorAcademicInfoWithRetry(forceRefresh: Boolean): DataState {
        var firstAttempt = true
        return retryExpiredDataState(
            maxRetryTimes = SCORE_MAX_RETRY_TIMES,
            retryIntervalMillis = SCORE_RETRY_INTERVAL,
            finalErrorMessage = "成绩明细生成较慢，请稍后下拉刷新"
        ) {
            updateMajorAcademicInfo(forceRefresh && firstAttempt).also { firstAttempt = false }
        }
    }

    private suspend fun getMinorScoreFromLocal() {
        academicDataRepository.getMinorScore()?.let {
            _minorScore.value = it
        }
    }

    suspend fun updateMinorAcademicInfo(forceRefresh: Boolean = false): DataState {
        return when (val result = academicRepository.getMinorAcademicInfo(forceRefresh)) {
            is NetworkResult.Success -> {
                academicDataRepository.updateAcademicData(
                    minorScore = result.data
                )
                getMinorScoreFromLocal()
                DataState.Newest
            }

            is NetworkResult.Error -> {
                codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateMinorAcademicInfoWithRetry(forceRefresh: Boolean): DataState {
        var firstAttempt = true
        return retryExpiredDataState(
            maxRetryTimes = SCORE_MAX_RETRY_TIMES,
            retryIntervalMillis = SCORE_RETRY_INTERVAL,
            finalErrorMessage = "辅修成绩生成较慢，请稍后下拉刷新"
        ) {
            updateMinorAcademicInfo(forceRefresh && firstAttempt).also { firstAttempt = false }
        }
    }

    private suspend fun getTotalRankFromLocal() {
        academicDataRepository.getTotalRank()?.let {
            _totalRank.value = it
        }
    }

    suspend fun updateTotalRank(forceRefresh: Boolean = false): DataState {
        return when (val result = academicRepository.getTotalRank(forceRefresh)) {
            is NetworkResult.Success -> {
                academicDataRepository.updateAcademicData(
                    totalRank = result.data
                )
                getTotalRankFromLocal()
                DataState.Newest
            }

            is NetworkResult.Error -> {
                codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateTotalRankWithRetry(forceRefresh: Boolean): DataState {
        var firstAttempt = true
        return retryExpiredDataState(
            maxRetryTimes = TOTAL_RANK_MAX_RETRY_TIMES,
            retryIntervalMillis = TOTAL_RANK_RETRY_INTERVAL,
            finalErrorMessage = "排名生成较慢，请稍后下拉刷新"
        ) {
            updateTotalRank(forceRefresh && firstAttempt).also { firstAttempt = false }
        }
    }

    private suspend fun getCompulsoryRankFromLocal() {
        academicDataRepository.getCompulsoryRank()?.let {
            _compulsoryRank.value = it
        }
    }

    suspend fun updateCompulsoryRank(forceRefresh: Boolean = false): DataState {
        return when (val result = academicRepository.getCompulsoryRank(forceRefresh)) {
            is NetworkResult.Success -> {
                academicDataRepository.updateAcademicData(
                    compulsoryRank = result.data
                )
                getCompulsoryRankFromLocal()
                DataState.Newest
            }

            is NetworkResult.Error -> {
                codeToDataState(result.code)
            }
        }
    }

    private suspend fun updateCompulsoryRankWithRetry(forceRefresh: Boolean): DataState {
        var firstAttempt = true
        return retryExpiredDataState(
            maxRetryTimes = COMPULSORY_RANK_MAX_RETRY_TIMES,
            retryIntervalMillis = COMPULSORY_RANK_RETRY_INTERVAL,
            finalErrorMessage = "必修排名生成较慢，请稍后下拉刷新"
        ) {
            updateCompulsoryRank(forceRefresh && firstAttempt).also { firstAttempt = false }
        }
    }

    fun resetLoadingState() {
        if (_majorAcademicInfoState.value is DataState.Loading) {
            _majorAcademicInfoState.value = DataState.Uninitialized
        }
        if (_minorAcademicInfoState.value is DataState.Loading) {
            _minorAcademicInfoState.value = DataState.Uninitialized
        }
        if (_compulsoryRankState.value is DataState.Loading) {
            _compulsoryRankState.value = DataState.Uninitialized
        }
        if (_totalRankState.value is DataState.Loading) {
            _totalRankState.value = DataState.Uninitialized
        }
    }
}
