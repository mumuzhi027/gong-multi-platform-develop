package com.sky31.gongmultiplatform.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val typography = Typography(
    titleLarge = TextStyle(
        fontSize = 26.sp,

        fontWeight = FontWeight(800)
    ),
    titleMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight(800)
    ),
    labelMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight(800)
    ),
    labelSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight(500)
    )
)