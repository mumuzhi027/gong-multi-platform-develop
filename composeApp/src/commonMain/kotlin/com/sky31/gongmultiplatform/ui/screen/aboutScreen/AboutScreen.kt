package com.sky31.gongmultiplatform.ui.screen.aboutScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.ui.component.ScaffoldTopBar
import com.sky31.gongmultiplatform.util.ClauseRoute
import com.sky31.gongmultiplatform.util.PlatformInfo
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.gonggong_logo
import gongmultiplatform.composeapp.generated.resources.permissions
import gongmultiplatform.composeapp.generated.resources.privacy_policy
import gongmultiplatform.composeapp.generated.resources.user_clause
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun AboutScreen(
    navController: NavController
) {
    val platformInfo = getKoin().get<PlatformInfo>()

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
                navController = LocalOverloadNavController.current,
                title = "关于"
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD2DC), Color(0x001970A6)), // 从蓝到透明
                                    center = Offset(with(LocalDensity.current) { 80.dp.toPx() }, with(LocalDensity.current) { 80.dp.toPx() }),
                                    radius = with(LocalDensity.current) { 80.dp.toPx() }
                                )
                            )
                    )

                    Image(
                        painter = painterResource(Res.drawable.gonggong_logo),
                        contentDescription = "logo",
                        modifier = Modifier
                            .size(160.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "拱拱",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "版本: ${platformInfo.getVersionName()}",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    MenuItem(
                        Res.drawable.privacy_policy,
                        "隐私政策",
                        "查看我们如何收集、使用、存储和保护您的个人信息"
                    ) {
                        navController.navigate(ClauseRoute("privacyPolicy", "隐私政策"))
                    }
                    Spacer(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.background))
                    MenuItem(
                        Res.drawable.permissions,
                        "权限申请与使用情况说明",
                        "查看我们申请的权限及其使用情况"
                    ) {
                        navController.navigate(
                            ClauseRoute(
                                "permissionsRequest",
                                "权限申请与使用情况说明"
                            )
                        )
                    }
                    Spacer(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.background))
                    MenuItem(
                        Res.drawable.user_clause,
                        "用户协议",
                        "使用拱拱之前，请务必仔细阅读并同意我们的用户协议"
                    ) {
                        navController.navigate(ClauseRoute("userClause", "用户条款"))
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: DrawableResource,
    title: String,
    content: String,
    click: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                click()
            }
            .padding(top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = "privacy_policy",
                modifier = Modifier
                    .size(20.dp)
            )

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Text(
            text = content,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall
        )
    }
}