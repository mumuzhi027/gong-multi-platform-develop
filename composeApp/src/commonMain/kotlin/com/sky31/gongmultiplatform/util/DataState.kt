package com.sky31.gongmultiplatform.util

sealed class DataState {
    data object Uninitialized : DataState()
    data object Loading : DataState()
    data object Newest : DataState()
    data object Expired : DataState()
    data class Error(val message: String) : DataState()
    data object Unauthorized: DataState()
}