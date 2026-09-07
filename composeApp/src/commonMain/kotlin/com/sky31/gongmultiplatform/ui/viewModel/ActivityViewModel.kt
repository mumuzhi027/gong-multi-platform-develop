package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky31.gongmultiplatform.model.CampusActivity
import com.sky31.gongmultiplatform.model.previewCampusActivities
import com.sky31.gongmultiplatform.network.repository.ActivityRepositoryImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActivityViewModel : ViewModel(), KoinComponent {
    private val repository: ActivityRepositoryImpl by inject()

    private val _activities = MutableStateFlow<List<CampusActivity>>(previewCampusActivities)
    val activities = _activities.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _usingFallback = MutableStateFlow(true)
    val usingFallback = _usingFallback.asStateFlow()

    private val _filters = MutableStateFlow<List<String>>(emptyList())
    val filters = _filters.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_loading.value) return

        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.getActivities()) {
                is NetworkResult.Success -> {
                    _activities.value = result.data
                    _usingFallback.value = false
                }

                is NetworkResult.Error -> {
                    _usingFallback.value = true
                }
            }
            when (val result = repository.getActivityFilters()) {
                is NetworkResult.Success -> _filters.value = result.data
                is NetworkResult.Error -> Unit
            }
            _loading.value = false
        }
    }
}
