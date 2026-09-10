package com.sky31.gongmultiplatform.ui.screen.launchScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.gonggong_logo
import gongmultiplatform.composeapp.generated.resources.login_logo
import gongmultiplatform.composeapp.generated.resources.teacher_day_dark
import gongmultiplatform.composeapp.generated.resources.teacher_day_light
import org.jetbrains.compose.resources.painterResource

@Composable
fun HolidayLaunchScreen(
    holiday: HolidayLaunch,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    when (holiday) {
        HolidayLaunch.TEACHERS_DAY -> TeachersDayLaunchScreen(
            isDarkTheme = isDarkTheme,
            modifier = modifier,
        )
    }
}

@Composable
private fun TeachersDayLaunchScreen(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFCFCFC)
    val taglineColor = if (isDarkTheme) Color(0xFFE8E8E8) else Color(0xFF505050)
    val blessingColor = if (isDarkTheme) Color(0xFFFFD2DE) else Color(0xFFE6537E)
    val footerColor = if (isDarkTheme) Color(0xFFBEBEBE) else Color(0xFF969696)
    val wordmarkBorderColor = if (isDarkTheme) Color(0xFFB9B9B9) else Color(0xFFD4D4D4)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        Image(
            painter = painterResource(
                if (isDarkTheme) Res.drawable.teacher_day_dark
                else Res.drawable.teacher_day_light
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.25f),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(104.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(22.dp),
                        clip = false,
                    )
                    .background(
                        color = Color(0xFFFFD0DC),
                        shape = RoundedCornerShape(22.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(Res.drawable.gonggong_logo),
                    contentDescription = "拱拱",
                    modifier = Modifier.size(94.dp),
                )
            }

            Spacer(modifier = Modifier.size(18.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(146.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = wordmarkBorderColor,
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                                ),
                            ),
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.login_logo),
                    contentDescription = "拱拱",
                    modifier = Modifier.width(126.dp),
                    contentScale = ContentScale.FillWidth,
                )
            }

            Spacer(modifier = Modifier.size(22.dp))

            Text(
                text = "—湘大变得如此简单—",
                color = taglineColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "教师节——祝福所有老师！",
                color = blessingColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "湘潭大学三翼 · 青媒融媒体中心 · 产品中心",
            color = footerColor,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 108.dp),
        )
    }
}
