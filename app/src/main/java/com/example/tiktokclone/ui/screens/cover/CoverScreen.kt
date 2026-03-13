package com.example.tiktokclone.ui.screens.cover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiktokclone.R
import kotlinx.coroutines.delay

@Composable
fun CoverScreen(onTimeout: () -> Unit) {
    val white = Color(0xFFF2F2F2)
    val pink = Color(0xFFFF2E63)

    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.creator_logo),
            contentDescription = "Creator Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.size(24.dp))

        Column {
            Text(
                text = "CREATOR",
                color = white,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "S U I T E",
                color = pink,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 5.sp
            )
        }
    }
}
