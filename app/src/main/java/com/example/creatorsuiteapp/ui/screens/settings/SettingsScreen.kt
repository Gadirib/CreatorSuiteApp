package com.example.creatorsuiteapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.data.repository.CleanerDayStat
import com.example.creatorsuiteapp.data.repository.CleanerStatsRepository
import com.example.creatorsuiteapp.data.tiktok.TikTokSessionManager
import com.example.creatorsuiteapp.ui.screens.cleaner.UnRepostViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val cleanerVm: UnRepostViewModel = viewModel()
    val deletionsToday by cleanerVm.deletionsToday.collectAsState()
    val session = remember { TikTokSessionManager.getSession(context) }
    val username = session?.username ?: "Username"
    val avatarUrl = session?.avatarUrl
    var totalCleaned by remember { mutableIntStateOf(0) }
    var weekStats by remember { mutableStateOf<List<CleanerDayStat>>(emptyList()) }
    var showRateFlow by remember { mutableStateOf(false) }

    LaunchedEffect(deletionsToday) {
        totalCleaned = CleanerStatsRepository.getTotalCleaned(context)
        weekStats = CleanerStatsRepository.getLast7DaysStats(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.height(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.creator_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CREATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("S U I T E", color = Color(0xFFA5A7C5), fontWeight = FontWeight.Medium, letterSpacing = 6.sp, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("✕", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onClose() })
        }

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1B1E2B)))
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141E), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF3A3D52), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFF2E63), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF8CA0B3), CircleShape)
                            .border(3.dp, Color(0xFFFF2E63), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                }

                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Hey, @$username 👋", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1A1D29), RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xFF30344B), RoundedCornerShape(18.dp))
                            .clickable {
                                TikTokSessionManager.clearSession(context)
                                val cm = CookieManager.getInstance()
                                cm.removeAllCookies(null)
                                cm.flush()
                                onLoggedOut()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Logout, null, tint = Color(0xFFFF2E63), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out", color = Color(0xFF8A8FA6), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            RecentPerformanceCard(
                totalCleaned = totalCleaned,
                weekStats = weekStats
            )
        }

        Spacer(Modifier.height(20.dp))
        SettingsRow("Share", Icons.Outlined.Share)
        Spacer(Modifier.height(12.dp))
        SettingsRow("Rate", Icons.Outlined.Star) { showRateFlow = true }
        Spacer(Modifier.height(12.dp))
        SettingsRow("Contact us", Icons.Outlined.Email)
    }

    if (showRateFlow) {
        RateFlowDialog(
            onDismiss = { showRateFlow = false },
            onRated = {
                showRateFlow = false
                onClose()
            }
        )
    }
}

@Composable
private fun RecentPerformanceCard(
    totalCleaned: Int,
    weekStats: List<CleanerDayStat>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF15161F), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2A2E3E), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("RESENT PERFORMANCE", color = Color(0xFF7D8198), fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                Text("Last 7 days", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Cleanup activity overview", color = Color(0xFF7D8198), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(totalCleaned.toString(), color = Color(0xFF27FFEA), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text("TOTAL CLEANED", color = Color(0xFF7D8198), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10111A), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val maxCount = (weekStats.maxOfOrNull { it.count } ?: 0).coerceAtLeast(5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = Color(0xFF1F2230)
                    repeat(5) { i ->
                        val y = size.height * i / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.5f)
                    }
                    repeat(6) { i ->
                        val x = size.width * i / 6f
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.5f)
                    }

                    if (weekStats.isNotEmpty()) {
                        val path = Path()
                        weekStats.forEachIndexed { index, stat ->
                            val x = if (weekStats.size == 1) 0f else size.width * index / (weekStats.size - 1).toFloat()
                            val normalized = stat.count / maxCount.toFloat()
                            val y = size.height - (normalized * size.height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF27E8F1), style = Stroke(width = 5f, cap = StrokeCap.Round))
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(maxCount, (maxCount * 3) / 4, maxCount / 2, maxCount / 4, 0).forEach {
                        Text(it.toString(), color = Color(0xFF8A8FA6), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                weekStats.ifEmpty {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                        Text(label, color = Color(0xFF8A8FA6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                weekStats.forEach { stat ->
                    Text(stat.dayLabel, color = Color(0xFF8A8FA6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF3A3D52), RoundedCornerShape(24.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF2A0D16), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFF2E63), modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(18.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private enum class RateStage {
    Initial,
    Selected
}

@Composable
private fun RateFlowDialog(
    onDismiss: () -> Unit,
    onRated: () -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(RateStage.Initial) }
    var rating by remember { mutableIntStateOf(0) }
    val imageRes = when (stage) {
        RateStage.Initial -> R.drawable.rate_1
        RateStage.Selected -> R.drawable.rate_12
    }

    fun openStore() {
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(marketIntent) }
            .recoverCatching { context.startActivity(webIntent) }
        onRated()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val screenWidth = maxWidth
            val imageWidth = if (screenWidth < 390.dp) screenWidth else 390.dp
            val imageHeight = imageWidth * 812f / 390f
            val horizontalPadding = (screenWidth - imageWidth) / 2f
            val topInsetMask = 52.dp

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(imageWidth)
                    .height(imageHeight),
                contentScale = ContentScale.FillWidth
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(imageWidth)
                    .height(topInsetMask)
                    .background(Color.Black)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = horizontalPadding + 138.dp, y = 108.dp)
                    .width(130.dp)
                    .height(36.dp)
                    .clickable { onDismiss() }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = horizontalPadding + 40.dp, y = 512.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 56.dp)
                            .clickable {
                                rating = index + 1
                                stage = RateStage.Selected
                            }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = horizontalPadding + 28.dp, y = 680.dp)
                    .width(imageWidth - 56.dp)
                    .height(64.dp)
                    .clickable(enabled = stage == RateStage.Selected && rating > 0) {
                        openStore()
                    }
            )

            if (stage == RateStage.Selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = horizontalPadding + 28.dp, y = 680.dp)
                        .width(imageWidth - 56.dp)
                        .height(64.dp)
                )
            }
        }
    }
}
