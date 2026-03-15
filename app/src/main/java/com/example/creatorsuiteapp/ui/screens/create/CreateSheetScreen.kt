package com.example.creatorsuiteapp.ui.screens.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R

@Composable
fun CreateSheetScreen(
    onClose: () -> Unit,
    onRecord: () -> Unit,
    onImport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Color.Black,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .border(
                    1.dp,
                    Color(0xFF31354A),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(56.dp)
                    .height(5.dp)
                    .background(Color(0xFF5B5E73), RoundedCornerShape(99.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Create",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CreateOptionCard(
                    modifier = Modifier.weight(1f),
                    title = "Record",
                    subtitle = "Video",
                    iconRes = R.drawable.ic_create_record,
                    c1 = Color(0xFFFF2E63),
                    c2 = Color(0xFFFF6D93),
                    onClick = onRecord
                )

                CreateOptionCard(
                    modifier = Modifier.weight(1f),
                    title = "Import",
                    subtitle = "Video",
                    iconRes = R.drawable.ic_create_import,
                    c1 = Color(0xFF3EA5FF),
                    c2 = Color(0xFF73BBFF),
                    onClick = onImport
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun CreateOptionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    iconRes: Int,
    c1: Color,
    c2: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(190.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(c1, c2),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                ),
                shape = RoundedCornerShape(34.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.25f),
                RoundedCornerShape(34.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 36.dp, y = 36.dp)
                .background(Color.White.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(105.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 22.dp, y = 24.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 14.dp)
                .background(Color.White.copy(alpha = 0.11f), CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
