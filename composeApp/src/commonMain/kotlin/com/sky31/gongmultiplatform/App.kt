package com.sky31.gongmultiplatform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sky31.gongmultiplatform.di.LocalIsDarkTheme
import com.sky31.gongmultiplatform.ui.navHost.AuthNavHost
import com.sky31.gongmultiplatform.ui.screen.launchScreen.HOLIDAY_LAUNCH_MIN_DISPLAY_MILLIS
import com.sky31.gongmultiplatform.ui.screen.launchScreen.HolidayLaunchHistory
import com.sky31.gongmultiplatform.ui.screen.launchScreen.HolidayLaunchScreen
import com.sky31.gongmultiplatform.ui.screen.launchScreen.currentHolidayLaunch
import kotlinx.coroutines.delay

@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppProvider {
            val holidayOccurrence = remember { currentHolidayLaunch() }
            var showHolidayLaunch by remember(holidayOccurrence) {
                mutableStateOf(
                    holidayOccurrence?.let(HolidayLaunchHistory::shouldShow) == true
                )
            }

            LaunchedEffect(holidayOccurrence, showHolidayLaunch) {
                if (holidayOccurrence != null && showHolidayLaunch) {
                    delay(HOLIDAY_LAUNCH_MIN_DISPLAY_MILLIS)
                    HolidayLaunchHistory.markShown(holidayOccurrence)
                    showHolidayLaunch = false
                }
            }

            Crossfade(
                targetState = showHolidayLaunch,
                animationSpec = tween(durationMillis = 250),
                label = "holiday launch screen",
            ) { showLaunch ->
                if (showLaunch && holidayOccurrence != null) {
                    HolidayLaunchScreen(
                        holiday = holidayOccurrence.holiday,
                        isDarkTheme = LocalIsDarkTheme.current,
                    )
                } else {
                    AuthNavHost()
                }
            }
        }
    }
}
