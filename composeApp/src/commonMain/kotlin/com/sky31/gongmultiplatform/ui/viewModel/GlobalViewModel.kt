package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.model.config.GlobalConfig
import com.sky31.gongmultiplatform.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GlobalViewModel: ViewModel(), KoinComponent {

    val configRepository: ConfigRepositoryImpl by inject()

    private val _globalConfig = MutableStateFlow(GlobalConfig())
    val globalConfig = _globalConfig.asStateFlow()

    init {
        viewModelScope.launch {
            loadConfig()
        }
    }

    suspend fun loadConfig() {
        val globalConfig = runCatching {
            configRepository.getGlobalConfig()
        }.getOrElse {
            println("Failed to load global config: ${it.message}")
            null
        } ?: GlobalConfig()
        _globalConfig.value = globalConfig
    }

    suspend fun updateConfig(
        themeMode: ThemeMode? = null
    ) {
        configRepository.updateConfig(themeMode)

        loadConfig()
    }
}
