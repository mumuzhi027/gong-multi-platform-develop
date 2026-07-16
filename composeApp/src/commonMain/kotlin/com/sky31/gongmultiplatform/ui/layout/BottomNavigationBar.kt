package com.sky31.gongmultiplatform.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.classroom
import gongmultiplatform.composeapp.generated.resources.grade
import gongmultiplatform.composeapp.generated.resources.home

@Composable
fun BottomNavigationBar(
    navController: NavController
) {
    val colorScheme = MaterialTheme.colorScheme
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val toHome = fun() {
        if (currentRoute !== "home") {
            navController.navigate("home")
        }
    }

    val toEmptyClassroom = fun() {
        if (currentRoute !== "emptyClassroom") {
            navController.navigate("emptyClassroom")
        }
    }

    val toAcademicScreen = fun() {
        if(currentRoute !== "academic") {
            navController.navigate("academic")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(75.dp)
            .drawBehind {
                drawLine(
                    color = colorScheme.surface,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavigationItem(
                isSelected = currentRoute == "home",
                resource = Res.drawable.home,
                title = "首页",
                onClick = toHome
            )

            BottomNavigationItem(
                isSelected = currentRoute == "emptyClassroom",
                resource = Res.drawable.classroom,
                title = "空教室",
                onClick = toEmptyClassroom
            )

            BottomNavigationItem(
                isSelected = currentRoute == "academic",
                resource = Res.drawable.grade,
                title = "成绩单",
                onClick = toAcademicScreen
            )
        }
    }

}
