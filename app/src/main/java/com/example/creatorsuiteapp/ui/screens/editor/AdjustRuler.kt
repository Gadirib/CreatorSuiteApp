package com.example.creatorsuiteapp.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdjustRuler(
    value: Float,
    onValueChange: (Float) -> Unit,
    accentPink: Color
) {
    val clamped = value.coerceIn(-100f, 100f)
    val fraction = (clamped + 100f) / 200f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-100", color = Color(0xFF6F738A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("100", color = Color(0xFF6F738A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val xPx = (fraction * widthPx).coerceIn(0f, widthPx)
            val xDp = with(density) { xPx.toDp() }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(31) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(if (it % 5 == 0) 22.dp else 14.dp)
                            .background(Color(0xFF747994), RoundedCornerShape(2.dp))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = xDp - 1.dp)
                    .align(Alignment.CenterStart)
                    .width(2.dp)
                    .height(66.dp)
                    .background(accentPink)
            )

            Box(
                modifier = Modifier
                    .offset(x = xDp - 28.dp)
                    .align(Alignment.TopStart)
                    .size(56.dp)
                    .border(2.dp, accentPink, CircleShape)
                    .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val label = if (clamped % 1f == 0f) clamped.toInt().toString() else String.format("%.1f", clamped)
                Text(label, color = accentPink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Slider(
                value = clamped,
                onValueChange = { onValueChange(it.coerceIn(-100f, 100f)) },
                valueRange = -100f..100f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}
