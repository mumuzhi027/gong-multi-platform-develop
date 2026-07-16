package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.model.config.AuthConfig
import com.sky31.gongmultiplatform.model.config.FunctionalConfig
import com.sky31.gongmultiplatform.model.config.NotificationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConfigViewModel: ViewModel(), KoinComponent {
    val configRepository: ConfigRepositoryImpl by inject()

    val notificationConfig = MutableStateFlow(NotificationConfig())
    val authConfig = MutableStateFlow(AuthConfig())

    init {
        viewModelScope.launch {
            loadConfig()
        }
    }

    suspend fun loadConfig() {
        val result = configRepository.getFunctionalConfig()

        if(result === null) {
            configRepository.insertConfig()
        } else {
            notificationConfig.value = result.notificationConfig
            authConfig.value = result.authConfig
        }
    }

    suspend fun updateFunctionalConfig() {
        val config = FunctionalConfig(
            notificationConfig = notificationConfig.value,
            authConfig = authConfig.value
        )

        println("update result is $config")

        configRepository.updateFunctionalConfig(config)
    }
}