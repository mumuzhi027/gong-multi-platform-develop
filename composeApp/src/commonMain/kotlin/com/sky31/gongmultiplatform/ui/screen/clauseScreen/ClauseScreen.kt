package com.sky31.gongmultiplatform.ui.screen.clauseScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.ui.component.CustomWebView
import com.sky31.gongmultiplatform.ui.component.ScaffoldTopBar

@Composable
fun ClauseScreen(
    navController: NavController,
    route: String,
    title: String
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )

    Scaffold(
        modifier = Modifier
            .safeDrawingPadding(),
        topBar = {
            ScaffoldTopBar(
                navController = navController,
                title = title
            )
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            CustomWebView("${SystemGlobalConfig.webEndpoint.baseUrl}#/$route")
        }
    }
}
