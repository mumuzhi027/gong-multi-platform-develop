package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.UserInfoDataRepositoryImpl
import com.sky31.gongmultiplatform.model.UserInfo
import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.network.repository.NotificationRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.UserInfoRepositoryImpl
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.PlatformInfo
import com.sky31.gongmultiplatform.util.codeToDataState
import com.sky31.gongmultiplatform.util.safeApiCallsSequential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DrawerViewModel: ViewModel(), KoinComponent {
    private val userInfoDataRepository: UserInfoDataRepositoryImpl by inject()
    val platformInfo: PlatformInfo by inject()

    private val userInfoRepository: UserInfoRepositoryImpl by inject()
    private val notificationRepository: NotificationRepositoryImpl by inject()

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo = _userInfo.asStateFlow()

    private val _updateData = MutableStateFlow<UpdateDto?>(null)
    val updateData = _updateData.asStateFlow()

    init {
        viewModelScope.launch {
            getUserInfoFromLocal()
        }
    }

    suspend fun update() {
        safeApiCallsSequential(
            calls = listOf { updateUserInfo() }
        )
    }

    private suspend fun getUserInfoFromLocal() {
        userInfoDataRepository.getUserInfo()?.let {
            _userInfo.value = it
        }
    }

    suspend fun updateUserInfo(): DataState {
        when(val result = userInfoRepository.getUserInfo()) {
            is NetworkResult.Success -> {
                val data = result.data
                userInfoDataRepository.insertUserInfo(
                    studentID = data.studentID,
                    name = data.name,
                    gender = data.gender,
                    birthday = data.birthday,
                    major = data.major,
                    clazz = data.clazz,
                    entranceDay = data.entranceDay
                )

                getUserInfoFromLocal()

                return DataState.Newest
            }

            is NetworkResult.Error ->
                return codeToDataState(result.code)
        }
    }

    suspend fun checkUpdate(): DataState {
        val result = notificationRepository.getUpdateNotification()

        when(result) {
            is NetworkResult.Success -> {
                _updateData.value = result.data
                return DataState.Newest
            }
            is NetworkResult.Error -> {
                println(result.toString())
                return DataState.Error(result.message)
            }
        }
    }
}