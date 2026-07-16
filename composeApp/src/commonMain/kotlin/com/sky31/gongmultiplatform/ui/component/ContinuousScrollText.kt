package com.sky31.gongmultiplatform.ui.component

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ContinuousScrollText(
    text: String,
    modifier: Modifier = Modifier.width(60.dp),
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = 1500,
                spacing = MarqueeSpacing(20.dp)
            ),
        overflow = TextOverflow.Visible,
        style = style
    )
}