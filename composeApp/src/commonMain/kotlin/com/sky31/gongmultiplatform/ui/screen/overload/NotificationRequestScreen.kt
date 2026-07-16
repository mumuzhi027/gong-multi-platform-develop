package com.sky31.gongmultiplatform.ui.screen.overload

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.util.askPostNotificationPermission
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.notification_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun NotificationRequestScreen() {
    val navController = LocalOverloadNavController.current

    Box(
        modifier = Modifier
            .safeDrawingPadding()
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(Res.drawable.notification_logo),
                contentDescription = "notification_logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(300.dp)
                    .height(120.dp)
            )

            Text(
                text = "开启通知",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = 50.dp)
            )

            Text(
                text = "接收课程、考试的通知及更多内容。",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 20.dp)
            )

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        askPostNotificationPermission {granted ->
                            if(granted) {
                                navController.popBackStack()
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                    .background(MaterialTheme.colorScheme.onBackground)
                    .padding(horizontal = 30.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "开启通知",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        navController.popBackStack()
                    }
                    .padding(horizontal = 30.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "以后再说",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = "在\"设置\"中随时管理通知类别。",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 100.dp)
            )
        }
    }
}