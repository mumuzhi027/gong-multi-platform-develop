package com.sky31.gongmultiplatform.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomNavigationItem(
    isSelected: Boolean,
    resource: DrawableResource,
    title: String,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val animatedScale = remember { Animatable(1.0f) }

    LaunchedEffect(isSelected) {
        if(isSelected) {
            scope.launch {
                animatedScale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = tween(durationMillis = 200)
                )

                animatedScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(50))
            .clickable {
                onClick()
            }
            .scale(animatedScale.value)
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = "home",
            tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(20.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
    }
}