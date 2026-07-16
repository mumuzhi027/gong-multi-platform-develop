package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.ui.viewModel.DrawerViewModel
import com.sky31.gongmultiplatform.ui.viewModel.MainViewModel
import com.sky31.gongmultiplatform.util.PlatformOperation
import com.sky31.gongmultiplatform.util.TokenState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.menu_icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    drawerState: DrawerState,
    toAcademicScreen: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = getKoin().get<MainViewModel>()
    val drawerViewModel: DrawerViewModel = viewModel { DrawerViewModel() }

    LaunchedEffect(Unit) {
        while(isActive) {
            viewModel.refreshCurrentTime()
            delay(60_000)
        }
    }

    LaunchedEffect(Unit) {
        if (TokenState.isOfflineMode.value) return@LaunchedEffect

        drawerViewModel.update()
    }

    PlatformOperation.BackHandler(true) {
        PlatformOperation.moveToBack()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.menu_icon),
                contentDescription = "menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .clickable {
                        scope.launch {
                            drawerState.open()
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
            ) {
                MainInfoBox(viewModel)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            VerticalCourseBox(viewModel)
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surface)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ExamArrangementBox(
                viewModel = viewModel,
                toAcademicScreen = toAcademicScreen
            )
        }
    }
}
