package com.sky31.gongmultiplatform.ui.component.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.ui.viewModel.DrawerViewModel
import com.sky31.gongmultiplatform.util.TokenState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.about
import gongmultiplatform.composeapp.generated.resources.settings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * mainScreen左侧栏
 *
 * @param state DrawerState
 */
@Composable
fun MainScreenDrawer(
    state: DrawerState
) {
    val overloadController = LocalOverloadNavController.current
    val viewModel: DrawerViewModel = viewModel { DrawerViewModel() }

    val scope = rememberCoroutineScope()

    val userInfo by viewModel.userInfo.collectAsState()

    LaunchedEffect(state.isOpen) {
        if(state.isOpen && !TokenState.isOfflineMode.value) {
            viewModel.update()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth(.75f)
            .clip(RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp))
            .background(MaterialTheme.colorScheme.background),
        bottomBar = {
            DrawerBottomBar(state)
        }
    ) { innerPadding ->
        val top = innerPadding.calculateTopPadding()
        val bottom = innerPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .padding(top = top, bottom = bottom, start = 8.dp, end = 8.dp)
        ) {
            DrawerUserInfo(
                modifier = Modifier
                    .fillMaxWidth(),
                userInfo = userInfo
            )

            IconRow(
                icon = Res.drawable.settings,
                text = "设置",
                click = {
                    scope.launch {
                        state.close()
                    }
                    overloadController.navigate("settings")
                }
            )

            IconRow(
                icon = Res.drawable.about,
                text = "关于",
                click = {
                    scope.launch {
                        state.close()
                    }
                    overloadController.navigate("about")
                }
            )
        }
    }
}

@Composable
fun IconRow(
    icon: DrawableResource,
    text: String,
    click: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                click()
            }
            .padding(top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "about",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
        )

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 10.dp)
        )
    }
}
