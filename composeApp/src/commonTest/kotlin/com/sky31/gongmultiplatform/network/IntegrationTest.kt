package com.sky31.gongmultiplatform.network

import com.sky31.gongmultiplatform.model.TokenData
import com.sky31.gongmultiplatform.network.repository.AuthRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertTrue

class IntegrationTest {

    @Test
    @Ignore
    fun getPublicTest() = runTest {
        val client = TestClientProvider.createIntegrationTest()

        val authRepository = AuthRepositoryImpl(client)
        val publicRepository = PublicRepositoryImpl(client)

        // Intentionally disabled by default. Run an external integration harness
        // with credentials injected from its secret store; never commit them.
        val loginResult = authRepository.login("INJECT_AT_RUNTIME", "INJECT_AT_RUNTIME")

        assertTrue { loginResult is NetworkResult.Success }

        TokenData.token = (loginResult as NetworkResult.Success).data.accessToken

        val todayClassroomResult = publicRepository.getTodayClassroom()

        println(todayClassroomResult)
    }
}
