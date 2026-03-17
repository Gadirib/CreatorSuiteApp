package com.example.creatorsuiteapp.ui.screens.saved

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.ui.components.PostRow
import com.example.creatorsuiteapp.ui.viewmodel.SavedViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedScreen(onClose: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    val vm: SavedViewModel = viewModel()
    val videos by vm.videos.collectAsState()
    val images by vm.images.collectAsState()
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_saved_download),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("SAVED FILES", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                Text("V I D E O  &  I M A G E S", color = Color(0xFF8F8CB0), letterSpacing = 2.sp, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22252C)))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedTab(
                title = "Video",
                selected = selectedTab == 0,
                iconRes = R.drawable.ic_video_small,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            SavedTab(
                title = "Images",
                selected = selectedTab == 1,
                iconRes = R.drawable.ic_search,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            SavedTab(
                title = "Posts",
                selected = selectedTab == 2,
                iconRes = R.drawable.ic_nav_content,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF44485A)))

        Spacer(modifier = Modifier.height(10.dp))

        if (loading) {
            Text("Loading...", color = Color(0xFF8A8FA6), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }
        if (error != null) {
            Text("Error: $error", color = Color(0xFFFF2E63), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(30) { index ->
                    val item = videos.getOrNull(index)
                    SavedGridCell(
                        thumbRes = item?.thumbRes,
                        isVideo = true
                    )
                }
            }
        } else if (selectedTab == 1) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(30) { index ->
                    val item = images.getOrNull(index)
                    SavedGridCell(
                        thumbRes = item?.imageRes,
                        isVideo = false
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    PostRow(post = post)
                }
            }
        }
    }
}

@Composable
private fun SavedTab(
    title: String,
    selected: Boolean,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = Color(0xFFFF2E63)
    val inactive = Color(0xFF8E91A8)

    Row(
        modifier = modifier
            .height(42.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (selected) active else inactive)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (selected) Color.White else inactive,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SavedGridCell(
    thumbRes: Int?,
    isVideo: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0C14))
    ) {
        if (thumbRes != null) {
            Image(
                painter = painterResource(thumbRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .background(Color(0xFFFF2E63), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
            )

            if (isVideo) {
                Text(
                    text = "00:00",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .background(Color(0xFF2A0E1B), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = Color(0xFF7B2A47), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
