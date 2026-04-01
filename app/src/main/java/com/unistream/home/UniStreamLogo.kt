package com.unistream.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.unistream.core.ui.theme.GalleryPrimaryLight
import com.unistream.core.ui.theme.NovelPrimaryLight
import com.unistream.core.ui.theme.StreamingPrimaryLight

@Composable
fun UniStreamLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        drawRoundRect(color = Color.White.copy(alpha = 0.12f), cornerRadius = CornerRadius(24f, 24f))
        val w = size.width
        val h = size.height
        drawArc(GalleryPrimaryLight, 160f, 220f, false, Offset(w * 0.12f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.75f), style = Stroke(10f))
        drawArc(StreamingPrimaryLight, 160f, 220f, false, Offset(w * 0.28f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.75f), style = Stroke(10f))
        drawArc(NovelPrimaryLight, 160f, 220f, false, Offset(w * 0.44f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.75f), style = Stroke(10f))
    }
}
