package com.example.creatorsuiteapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R

@Composable
fun SavedChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .background(Color(0xFF123F35), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1C5F52), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_saved_download),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(Color(0xFF00F2C3))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Saved",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun FeatureCard(
    countCreators: Int = 0,
    countContent: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF4A0D1E), Color(0xFF22060F))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color(0xFF6A2A3C), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF571527), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profile_generator),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text("Profile Image Generator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                text = "Creators $countCreators • Posts $countContent",
                color = Color(0xFFA19BB2),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SmallToolCard(
    modifier: Modifier,
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Column(
        modifier = modifier
            .height(160.dp)
            .background(Color(0xFF12141E), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2B2E3E), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(iconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                colorFilter = ColorFilter.tint(iconTint)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, color = Color(0xFF7C8096), fontSize = 10.sp)
    }
}

@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    c1: Color,
    c2: Color
) {
    Row(
        modifier = modifier
            .height(76.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(c1, c2)),
                shape = RoundedCornerShape(18.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun LoginButton(text: String, borderColor: Color, iconRes: Int) {
    androidx.compose.material3.OutlinedButton(
        onClick = { },
        modifier = Modifier
            .height(46.dp)
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF1C1C1C),
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}
