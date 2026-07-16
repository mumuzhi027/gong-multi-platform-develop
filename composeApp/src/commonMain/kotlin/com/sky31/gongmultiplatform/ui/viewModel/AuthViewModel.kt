package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.data.repository.AcademicDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ExamDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.UserInfoDataRepositoryImpl
import com.sky31.gongmultiplatform.di.viewModelModule
import com.sky31.gongmultiplatform.model.bearerTokenStorage
import com.sky31.gongmultiplatform.network.HttpClientProvider
import com.sky31.gongmultiplatform.network.dto.AuthDto
import com.sky31.gongmultiplatform.network.repository.AuthRepositoryImpl
import com.sky31.gongmultiplatform.util.AuthState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.TokenState
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules

class AuthViewModel: ViewModel(), KoinComponent {
    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val academicDataRepository: AcademicDataRepositoryImpl by inject()
    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val examDataRepository: ExamDataRepositoryImpl by inject()
    private val userInfoDataRepository: UserInfoDataRepositoryImpl by inject()

    private val authRepository: AuthRepositoryImpl by inject()
    private val settings: Settings by inject()

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    private val navigationChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEventsChannelFlow = navigationChannel.receiveAsFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    init {
        loadAuthState()
        _username.value = runCatching {
            settings.getStringOrNull("username")
        }.getOrElse {
            println("Failed to load username from local settings: ${it.message}")
            null
        }
    }

    private fun syncAccessToken(token: String) {
        bearerTokenStorage.clear()
        bearerTokenStorage.add(BearerTokens(token, ""))
        HttpClientProvider.client.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private fun clearAccessToken() {
        bearerTokenStorage.clear()
        HttpClientProvider.client.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private fun loadAuthState() {
        val token = runCatching {
            settings.getStringOrNull("token")
        }.getOrElse {
            println("Failed to load token from local settings: ${it.message}")
            null
        }
        println("load from local: $token")

        if(token != null) {
            syncAccessToken(token)
            _authState.value.isAuthenticated = true
        } else {
            _authState.value.isAuthenticated = false
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState()
    }

    suspend fun enterOfflineMode(needNavigation: Boolean = true) {
        clearAccessToken()
        TokenState.enterOfflineMode()
        _authState.value = _authState.value.copy(
            isLoading = false,
            isAuthenticated = true,
            errorMessage = null
        )
        if (needNavigation) {
            navigationChannel.send(NavigationEvent.ToMainScreen)
        }
    }

    suspend fun login(
        username: String,
        password: String,
        options: LoginOptions = LoginOptions(),
        onResult: (NetworkResult<AuthDto>) -> Unit = { }
    ) {
        _authState.value = _authState.value.copy(isLoading = true)

        val result = authRepository.login(username, password)
        when(result) {
            is NetworkResult.Success -> {
                syncAccessToken(result.data.accessToken)
                TokenState.refreshed()
                settings.putString("token", result.data.accessToken)
                settings.putString("username", username)
                _username.value = username
                _authState.value = _authState.value.copy(isLoading = false, isAuthenticated = true)

                if(options.needNavigation) {
                    navigationChannel.send(NavigationEvent.ToMainScreen)
                }
            }

            is NetworkResult.Error -> {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = result.message.ifBlank { "未知错误" }
                )
            }
        }

        onResult(result)
    }

    suspend fun logout() {
        publicDataRepository.deleteAllPublicData()
        academicDataRepository.deleteAllAcademicData()
        courseDataRepository.deleteAllCourses()
        examDataRepository.deleteAllExams()
        userInfoDataRepository.deleteAllUserInfo()

        clearAccessToken()
        TokenState.refreshed()
        settings.remove("token")
        settings.remove("username")

        unloadKoinModules(viewModelModule)
        loadKoinModules(viewModelModule)

        resetAuthState()
        navigationChannel.send(NavigationEvent.ToLoginScreen)
    }
}

data class LoginOptions(
    val needNavigation: Boolean = true
)

sealed class NavigationEvent {
    data object ToLoginScreen: NavigationEvent()
    data object ToMainScreen: NavigationEvent()
}
