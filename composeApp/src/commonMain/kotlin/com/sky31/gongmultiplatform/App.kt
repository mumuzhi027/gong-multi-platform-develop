package com.sky31.gongmultiplatform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sky31.gongmultiplatform.ui.navHost.AuthNavHost

@Composable
fun App() {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppProvider {
            AuthNavHost()
        }
    }
}