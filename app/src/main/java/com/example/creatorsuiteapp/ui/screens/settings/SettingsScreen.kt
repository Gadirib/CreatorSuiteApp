package com.example.creatorsuiteapp.ui.screens.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creatorsuiteapp.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import com.example.creatorsuiteapp.ui.viewmodel.ContentViewModel

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val contentVm: ContentViewModel = viewModel()
    val me by contentVm.me.collectAsState()

    LaunchedEffect(Unit) {
        if (me == null) contentVm.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Spacer(Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.creator_logo),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("S U I T E", color = Color(0xFFA5A7C5), fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("✕", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onClose() })
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))
        Spacer(Modifier.height(14.dp))

        // Profile card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141E), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.sample_img_1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFF2E63), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val username = me?.name ?: "@Username"
                    Text("Hey, $username 👋", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1A1D29), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF2C3042), RoundedCornerShape(16.dp))
                            .clickable {
                                onLoggedOut()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF2E63),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out", color = Color(0xFF8A8FA6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Performance card (static fake chart)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141E), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recent performance", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Last 7 days", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Cleanup activity overview", color = Color(0xFF7D8198), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("114", color = Color(0xFF2EE6A6), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("TOTAL CLEANED", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111A), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // grid
                    val gridColor = Color(0xFF1D2130)
                    repeat(4) { i ->
                        val y = h * (i + 1) / 5f
                        drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 2f)
                    }
                    repeat(6) { i ->
                        val x = w * (i + 1) / 7f
                        drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 2f)
                    }

                    // line
                    val path = Path().apply {
                        moveTo(0f, h * 0.7f)
                        cubicTo(w * 0.15f, h * 0.2f, w * 0.35f, h * 0.8f, w * 0.55f, h * 0.45f)
                        cubicTo(w * 0.7f, h * 0.3f, w * 0.85f, h * 0.9f, w, h * 0.35f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF2EE6E6),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(day, color = Color(0xFF7D8198), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsRow("Share", Icons.Outlined.Share)
        Spacer(Modifier.height(10.dp))
        SettingsRow("Rate", Icons.Outlined.Star)
        Spacer(Modifier.height(10.dp))
        SettingsRow("Contact us", Icons.Outlined.Email)
    }
}

@Composable
private fun SettingsRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color(0xFF12141E), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF2A0D16), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF2E63),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
