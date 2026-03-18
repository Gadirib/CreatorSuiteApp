package com.example.creatorsuiteapp.ui.screens.settings

import android.content.Context
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
import com.example.creatorsuiteapp.data.repository.RepostStatsRepository
import com.example.creatorsuiteapp.data.repository.SavedVideoRepository
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import kotlinx.coroutines.launch

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

    var showRatingDialog by remember { mutableStateOf(false) }
    var showThankYou by remember { mutableStateOf(false) }
    var selectedStars by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
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

                                    // 3. Clear in-memory video list (don't show previous account's videos)
                                    kotlinx.coroutines.GlobalScope.launch {
                                        SavedVideoRepository.switchAccount(context)
                                    }
                                    RepostStatsRepository.switchAccount(context)

                                    // 4. Navigate to login screen
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

            // ── Performance card — account-scoped real data ───────────────────────
            // Load stats for current account
            val repostStats by RepostStatsRepository.stats.collectAsState()
            LaunchedEffect(Unit) { RepostStatsRepository.load(context) }
            val totalCleaned = repostStats?.deletedCount ?: 0

            // Load per-day deletions for last 7 days from cleaner_prefs
            val prefs = context.getSharedPreferences("cleaner_prefs", Context.MODE_PRIVATE)
            val accountKey = run {
                val secUid = context.getSharedPreferences("tiktok_session", Context.MODE_PRIVATE)
                    .getString("secUid", null)
                if (!secUid.isNullOrBlank()) secUid.takeLast(16) else "default"
            }
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date())

            // Today's count — refreshes every time screen is visible
            val todayCount = prefs.getInt("count_$accountKey", 0).let { count ->
                val savedDate = prefs.getString("date_$accountKey", "")
                if (savedDate == todayStr) count else 0
            }
            val dailyLimit = 50
            val remaining = (dailyLimit - todayCount).coerceAtLeast(0)
            val usedFraction = (todayCount.toFloat() / dailyLimit).coerceIn(0f, 1f)

            // Read last 7 days counts
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            val calendar = java.util.Calendar.getInstance()
            val dailyCounts = (6 downTo 0).map { daysAgo ->
                calendar.time = java.util.Date()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                val dateKey = sdf.format(calendar.time)
                prefs.getInt("day_${accountKey}_$dateKey", 0).toFloat()
            }
            val maxCount = dailyCounts.maxOrNull()?.takeIf { it > 0f } ?: 1f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12141E), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                // ── Header row: Today + Total ─────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recent performance", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Last 7 days", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Cleanup activity overview", color = Color(0xFF7D8198), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$totalCleaned", color = Color(0xFF2EE6A6), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("TOTAL CLEANED", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Today's daily counter ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F111A), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TODAY", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 2.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "$todayCount cleaned",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Resets at midnight",
                                    color = Color(0xFF6F738A),
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$remaining left",
                                    color = if (remaining > 10) Color(0xFF2EE6A6) else Color(0xFFFF2E63),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "of $dailyLimit daily",
                                    color = Color(0xFF7D8198),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF1D2130), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(usedFraction)
                                    .height(6.dp)
                                    .background(
                                        when {
                                            usedFraction < 0.7f -> Color(0xFF2EE6A6)
                                            usedFraction < 0.9f -> Color(0xFFFFBB00)
                                            else -> Color(0xFFFF2E63)
                                        },
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F111A), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    // Bar chart — one bar per day, height proportional to deletions
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyCounts.forEachIndexed { i, count ->
                            val fraction = (count / maxCount).coerceIn(0f, 1f)
                            val barHeight = (fraction * 100f + 4f).dp // min 4dp so bar is always visible
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(barHeight)
                                    .background(
                                        if (count > 0f) Color(0xFF2EE6A6) else Color(0xFF1D2130),
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        dayLabels.forEach { day ->
                            Text(day, color = Color(0xFF7D8198), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsRow("Share", Icons.Outlined.Share)
            Spacer(Modifier.height(10.dp))
            SettingsRow("Rate", Icons.Outlined.Star, onClick = { showRatingDialog = true })
            Spacer(Modifier.height(10.dp))
            SettingsRow("Contact us", Icons.Outlined.Email)
        } // end Column
    } // end Box wrapper

    // Full-screen overlays — rendered outside scroll/column
    if (showRatingDialog) {
        RatingDialog(
            selectedStars = selectedStars,
            onStarSelect = { selectedStars = it },
            onSubmit = { showRatingDialog = false; showThankYou = true },
            onDismiss = { showRatingDialog = false }
        )
    }
    if (showThankYou) {
        ThankYouOverlay(stars = selectedStars, onDismiss = { showThankYou = false })
    }
}

@Composable
private fun SettingsRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth().height(70.dp)
            .background(Color(0xFF12141E), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(18.dp))
            .clickable { onClick() }
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

// ── Rating Screen (full screen, matches Figma) ───────────────────────────────
@Composable
private fun RatingDialog(
    selectedStars: Int,
    onStarSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentPink = Color(0xFFFF2E63)
    val starGold = Color(0xFFFFBB00)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background — dark gradient with warm overlay matching Figma
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF1A0A00), Color(0xFF0D0000), Color(0xFF000000))
                    )
                )
        )
        // Warm amber overlay top half — mimics the lit photo bg
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0x55C85A00), Color(0x22FF4400), Color(0x00000000))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // MAYBE LATER
            Text(
                "MAYBE LATER",
                color = Color.White.copy(0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp),
                style = androidx.compose.ui.text.TextStyle(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            )

            Spacer(Modifier.weight(1f))

            // HOW IS THE VIBE? — big bold text
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "HOW IS",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    lineHeight = 52.sp
                )
                Text(
                    "THE VIBE?",
                    color = accentPink,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    lineHeight = 52.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // Star boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                (1..5).forEach { star ->
                    val filled = star <= selectedStars
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(
                                if (filled) Color(0xFF2A1F00)
                                else Color(0xFF1A1A1A)
                            )
                            .border(
                                1.5.dp,
                                if (filled) starGold else Color(0xFF3A3A3A),
                                androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                            )
                            .clickable { onStarSelect(star) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (filled) "★" else "☆",
                            fontSize = 32.sp,
                            color = if (filled) starGold else Color(0xFF555555)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Rate button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(
                        if (selectedStars > 0) accentPink
                        else Color(0xFF6B1525)
                    )
                    .clickable(enabled = selectedStars > 0) { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Rate",
                    color = if (selectedStars > 0) Color.White else Color.White.copy(0.5f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
// ── Thank You Screen (full screen) ───────────────────────────────────────────
@Composable
private fun ThankYouOverlay(stars: Int, onDismiss: () -> Unit) {
    val accentPink = Color(0xFFFF2E63)
    val starGold = Color(0xFFFFBB00)

    Box(modifier = Modifier.fillMaxSize()) {
        // Same background style as rating screen
        Box(
            modifier = Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF000000))
                )
            )
        )
        // Green success tint
        Box(
            modifier = Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(Color(0x222EE6A6), Color(0x00000000)),
                    radius = 800f
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // MAYBE LATER placeholder (invisible, keeps layout consistent)
            Text("", modifier = Modifier.height(32.dp))

            Spacer(Modifier.weight(0.8f))

            // Big checkmark
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFF0D2B1A))
                    .border(2.dp, Color(0xFF2EE6A6).copy(0.4f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color(0xFF2EE6A6), fontSize = 64.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            // THANK YOU text
            Text(
                "THANK YOU",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                if (stars == 5) "FOR THE LOVE! 🎉" else "FOR THE FEEDBACK!",
                color = accentPink,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(24.dp))

            // Stars display
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..5).forEach { star ->
                    Text(
                        if (star <= stars) "★" else "☆",
                        fontSize = 36.sp,
                        color = if (star <= stars) starGold else Color(0xFF333333)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                when {
                    stars == 5 -> "You made our day!\nWe'll keep making\nCreator Suite better for you."
                    stars >= 3 -> "Your feedback means a lot!\nWe're always working\nto improve."
                    else -> "Thanks for being honest.\nWe'll use your feedback\nto get better."
                },
                color = Color(0xFF7D8198),
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.weight(1f))

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(58.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(accentPink)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("Done!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}