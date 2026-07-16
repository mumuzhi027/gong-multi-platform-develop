package com.sky31.gongmultiplatform.ui.component.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.model.UserInfo

@Composable
fun DrawerUserInfo(
    modifier: Modifier = Modifier,
    userInfo: UserInfo
) {

    Box(
        modifier = modifier
            .padding(top = 5.dp, bottom = 10.dp)
            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10f))
            .padding(top = 8.dp, bottom = 8.dp, start = 10.dp, end = 10.dp)
    ) {
        Column {
            Text(
                text = userInfo.name,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = userInfo.major,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = userInfo.studentID,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}