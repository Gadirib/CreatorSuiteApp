package com.example.creatorsuiteapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R

@Composable
fun BottomNav(
    active: String,
    onContentClick: () -> Unit,
    onCenterClick: () -> Unit,
    onCleanerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(113.dp)
            .background(Color.Black)
            .border(1.dp, Color(0xFF1F2230), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onContentClick() }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_nav_content),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(if (active == "CONTENT") Color(0xFFFF2E63) else Color(0xFF7E819A))
            )
            Text(
                "CONTENT",
                color = if (active == "CONTENT") Color(0xFFFF2E63) else Color(0xFF7E819A),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFF2E63), RoundedCornerShape(32.dp))
                .clickable { onCenterClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onCleanerClick() }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_clean_feed),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                colorFilter = ColorFilter.tint(if (active == "CLEANER") Color(0xFFFF2E63) else Color(0xFF7E819A))
            )
            Text(
                "CLEANER",
                color = if (active == "CLEANER") Color(0xFFFF2E63) else Color(0xFF7E819A),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
