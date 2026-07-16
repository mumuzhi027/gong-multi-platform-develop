package com.sky31.gongmultiplatform.network

import com.sky31.gongmultiplatform.model.TokenData
import com.sky31.gongmultiplatform.network.repository.AuthRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IntegrationTest {

    @Test
    fun getPublicTest() = runTest {
        val client = TestClientProvider.createIntegrationTest()

        val authRepository = AuthRepositoryImpl(client)
        val publicRepository = PublicRepositoryImpl(client)

        val loginResult = authRepository.login("202105650301", "13586591252Xx_")

        assertTrue { loginResult is NetworkResult.Success }

        TokenData.token = (loginResult as NetworkResult.Success).data.accessToken

        val todayClassroomResult = publicRepository.getTodayClassroom()

        println(todayClassroomResult)
    }
}