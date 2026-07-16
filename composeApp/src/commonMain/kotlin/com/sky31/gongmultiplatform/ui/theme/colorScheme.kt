package com.sky31.gongmultiplatform.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val lightColorScheme = lightColorScheme(
    primary = Color(0xFF85CDFA),
    onPrimary = Color.Black,
    background = Color(0xFFF1F1F1),
    onBackground = Color.Black,
    surface = Color(0xFFE3E3E3),
    onSurface = Color.Black,
    inverseOnSurface = Color.White,
    onSurfaceVariant = Color(0xFF515151),
    primaryContainer = Color(0xFFFFFFFF),
)

val darkColorScheme = darkColorScheme(
    primary = Color(0xFF3BB1FA),
    onPrimary = Color.White,
    background = Color(0xFF1C1C1C),
    onBackground = Color.White,
    surface = Color(0xFF313131),
    onSurface = Color.White,
    inverseOnSurface = Color.Black,
    onSurfaceVariant = Color(0xFFB9B9B9),
    primaryContainer = Color(0xFF000000),
)