package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.util.AnimationState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_refresh_24
import org.jetbrains.compose.resources.painterResource

@Composable
fun DataLoadingRing(
    size: Dp = 20.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onBackground,
    state: AnimationState = AnimationState.Unstarted,
) {

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            scaleIn(
                initialScale = 0.2f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 100
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 100
                )
            ) togetherWith
            scaleOut(
                targetScale = 0.2f,
                animationSpec = tween(
                    durationMillis = 300
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 300
                )
            )
        },
        label = "refreshIconTransition"
    ) { target ->
        if (target !== AnimationState.Loading) {
            Icon(
                painter = painterResource(Res.drawable.baseline_refresh_24),
                contentDescription = "refresh",
                tint = color,
                modifier = Modifier
                    .size(size)
            )
        } else {
            LoadingRing(
                strokeWidth = strokeWidth
            )
        }
    }
}