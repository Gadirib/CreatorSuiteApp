package com.example.creatorsuiteapp.ui.screens.rec

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.ui.viewmodel.PublishStage
import com.example.creatorsuiteapp.ui.viewmodel.PublishUiState

@Composable
fun RecToolButton(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFFF2E63) else Color(0xFF171923)
    Column(
        modifier = Modifier
            .width(86.dp)
            .height(92.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(Color(0xFFB8B7D4))
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF222633), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Color(0xFF6E748C), fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    minValue: Int = 0,
    maxValue: Int = 10,
    onValueChange: (Float) -> Unit
) {
    val displayValue = (minValue + (value * (maxValue - minValue))).toInt()

    Text(title, color = Color(0xFF8A8FA6), fontWeight = FontWeight.Bold, fontSize = 11.sp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFFFF2E63),
                inactiveTrackColor = Color(0xFF2A2D3A),
                thumbColor = Color(0xFFFF2E63)
            )
        )
        Text(displayValue.toString(), color = Color(0xFF8A8FA6), fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(minValue.toString(), color = Color(0xFF6E748C), fontSize = 10.sp)
        Text(((minValue + maxValue) / 2).toString(), color = Color(0xFF6E748C), fontSize = 10.sp)
        Text(maxValue.toString(), color = Color(0xFF6E748C), fontSize = 10.sp)
    }
}

@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .background(if (selected) Color(0xFFFF2E63) else Color(0xFF171923), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun SaveVideoSheet(
    onNewVideo: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onPost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .border(1.dp, Color(0xFF363A4E), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(64.dp)
                .height(5.dp)
                .background(Color(0xFF646982), RoundedCornerShape(99.dp))
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Saving Video",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(Color.Black, RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(20.dp))
                .clickable { onNewVideo() }
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📹", color = Color(0xFFFF2E63), fontSize = 20.sp)
            Spacer(Modifier.width(14.dp))
            Text("New Video", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .background(Color(0xFF171923), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF2E3243), RoundedCornerShape(20.dp))
                    .clickable { onDiscard() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕  Discard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .weight(1.35f)
                    .height(84.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(20.dp))
                    .clickable { onSave() },
                contentAlignment = Alignment.Center
            ) {
                Text("⬇  Save Video", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(Color(0xFF350817), RoundedCornerShape(26.dp))
                .border(1.dp, Color(0xFFFF2E63), RoundedCornerShape(26.dp))
                .clickable { onPost() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("♪   Post to TikTok", color = Color(0xFFFF2E63), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(Modifier.height(8.dp))
    }
}
