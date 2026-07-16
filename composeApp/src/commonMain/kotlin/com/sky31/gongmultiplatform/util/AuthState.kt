package com.sky31.gongmultiplatform.util

data class AuthState(
    var isAuthenticated: Boolean = false,
    var isLoading: Boolean = false,
    var errorMessage: String? = null
)
