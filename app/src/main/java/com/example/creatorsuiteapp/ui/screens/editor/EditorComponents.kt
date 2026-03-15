package com.example.creatorsuiteapp.ui.screens.editor

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R

@Composable
fun TextFontChip(title: String, selected: Boolean, onClick: () -> Unit, fontSize: TextUnit) {
    Text(
        text = title,
        color = if (selected) Color(0xFFFF2E63) else Color(0xFF6F738A),
        fontSize = fontSize,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun TextColorDot(color: Color, selected: Boolean, onClick: () -> Unit, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
            .padding(3.dp)
            .background(color, CircleShape)
            .clickable { onClick() }
    )
}

fun textStyleFor(style: String, color: Color): TextStyle {
    return when (style) {
        "Style 1" -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, color = color)
        "Style 2" -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = color)
        "Style 3" -> TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color)
        "Style 4" -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, color = color)
        "Style 5" -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = color)
        "Style 6" -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = color)
        else -> TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun AspectRatioCard(
    modifier: Modifier = Modifier,
    ratio: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardHeight = when (ratio) {
        "Original" -> 130.dp
        "1:1" -> 130.dp
        "5:4" -> 130.dp
        "9:16" -> 190.dp
        "16:9" -> 110.dp
        else -> 130.dp
    }

    val iconBoxModifier = when (ratio) {
        "9:16" -> Modifier.size(width = 42.dp, height = 72.dp)
        "16:9" -> Modifier.size(width = 72.dp, height = 42.dp)
        else -> Modifier.size(64.dp)
    }

    Box(
        modifier = modifier
            .height(cardHeight)
            .background(
                if (selected) Color(0xFF7B1B35) else Color(0xFF1A1D29),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (selected) Color(0xFFFF2E63) else Color(0xFF2B3043),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(R.drawable.sample_video_1_thumb),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (selected) 0.45f else 0.35f
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .then(iconBoxModifier)
                .border(3.dp, Color.White, RoundedCornerShape(14.dp))
        )

        Text(
            text = ratio,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
fun TrimSplitBtn(
    modifier: Modifier,
    text: String,
    iconRes: Int,
    selected: Boolean,
    selectedColor: Color = Color(0xFFFF2E63),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                if (selected) selectedColor else Color(0xFF141722),
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (selected) selectedColor else Color(0xFF2D3348),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomEditorItem(
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) Color(0xFFFF2E63) else Color.White
    val bg = if (selected) Color(0xFF6B1329) else Color.Transparent

    Column(
        modifier = Modifier
            .width(66.dp)
            .height(66.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
