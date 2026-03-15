package com.example.creatorsuiteapp.ui.screens.settings

import android.webkit.CookieManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current

    // ✅ Read username directly from TikTokSessionManager — not ContentViewModel
    val session = remember { TikTokSessionManager.getSession(context) }
    val username = session?.username ?: "Username"
    val avatarUrl = session?.avatarUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Spacer(Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.creator_logo), null, modifier = Modifier.size(34.dp))
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

        // ── Profile card ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141E), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Avatar — use network image if available, fallback to placeholder
                if (!avatarUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFF2E63), CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Image(
                        painterResource(R.drawable.sample_img_1), null,
                        modifier = Modifier.size(54.dp).clip(CircleShape).border(2.dp, Color(0xFFFF2E63), CircleShape)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // ✅ Show real TikTok username with @ prefix
                    Text("Hey, @$username 👋", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(6.dp))

                    // ✅ Real logout — clears session AND cookies so next login requires credentials
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1A1D29), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF2C3042), RoundedCornerShape(16.dp))
                            .clickable {
                                // 1. Clear saved session (username, secUid, avatar)
                                TikTokSessionManager.clearSession(context)

                                // 2. Clear ALL TikTok cookies — this is the key step
                                // Without this, WebView remembers the session and skips login
                                val cm = CookieManager.getInstance()
                                cm.removeAllCookies(null)
                                cm.flush()

                                // 3. Navigate to login screen
                                onLoggedOut()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Logout, null, tint = Color(0xFFFF2E63), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out", color = Color(0xFF8A8FA6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Performance card ──────────────────────────────────────────────────
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
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val gridColor = Color(0xFF1D2130)
                    repeat(4) { i ->
                        val y = h * (i + 1) / 5f
                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 2f)
                    }
                    repeat(6) { i ->
                        val x = w * (i + 1) / 7f
                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), 2f)
                    }
                    val path = Path().apply {
                        moveTo(0f, h * 0.7f)
                        cubicTo(w * 0.15f, h * 0.2f, w * 0.35f, h * 0.8f, w * 0.55f, h * 0.45f)
                        cubicTo(w * 0.7f, h * 0.3f, w * 0.85f, h * 0.9f, w, h * 0.35f)
                    }
                    drawPath(path, Color(0xFF2EE6E6), style = Stroke(4f, cap = StrokeCap.Round))
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
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
            .fillMaxWidth().height(70.dp)
            .background(Color(0xFF12141E), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(Color(0xFF2A0D16), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFF2E63), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}