package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.model.GongAiChatRequest
import com.sky31.gongmultiplatform.model.GongAiUiMessage
import com.sky31.gongmultiplatform.network.repository.GongAiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GongAiViewModel : ViewModel(), KoinComponent {
    private val repository: GongAiRepository by inject()
    private val settings: Settings by inject()

    private val _messages = MutableStateFlow(
        listOf(
            GongAiUiMessage(
                id = 1,
                fromUser = false,
                text = "你好，我是拱拱AI。你可以问我课表、成绩、考试和空教室，也可以让我协助准备请假或场地预约。"
            )
        )
    )
    val messages = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _introAccepted = MutableStateFlow(settings.getBoolean("gong_ai_intro_accepted", false))
    val introAccepted = _introAccepted.asStateFlow()

    private var conversationId: String? = null
    private var nextId = 2L

    fun acceptIntro() {
        settings.putBoolean("gong_ai_intro_accepted", true)
        _introAccepted.value = true
    }

    fun send(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty() || _loading.value) return
        _messages.value = _messages.value + GongAiUiMessage(nextId++, true, text = normalized)
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            repository.chat(GongAiChatRequest(conversationId, normalized))
                .onSuccess { response ->
                    conversationId = response.conversationId
                    _messages.value = _messages.value + GongAiUiMessage(nextId++, false, blocks = response.blocks)
                }
                .onFailure { failure -> _error.value = failure.message ?: "请求失败，请稍后再试" }
            _loading.value = false
        }
    }

    fun confirm(actionId: String) {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            repository.confirm(actionId)
                .onSuccess { response ->
                    _messages.value = _messages.value + GongAiUiMessage(nextId++, false, blocks = listOf(response.block))
                }
                .onFailure { failure -> _error.value = failure.message ?: "确认失败" }
            _loading.value = false
        }
    }

    fun cancel(actionId: String) {
        viewModelScope.launch {
            repository.cancel(actionId)
                .onSuccess {
                    _messages.value = _messages.value + GongAiUiMessage(nextId++, false, text = "已取消，本次操作没有提交到校园系统。")
                }
                .onFailure { failure -> _error.value = failure.message ?: "取消失败" }
        }
    }

    fun clearConversation() {
        val current = conversationId
        conversationId = null
        _messages.value = _messages.value.take(1)
        _error.value = null
        if (current != null) viewModelScope.launch { repository.clearConversation(current) }
    }
}

