package com.example.creatorsuiteapp.ui.screens.action

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.ui.components.ActionTile

@Composable
fun ActionScreen(onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Image(
            painter = painterResource(id = R.drawable.creator_logo),
            contentDescription = "Creator Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CREATOR",
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "S U I T E",
            color = Color(0xFFFF2E63),
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Record, Edit & Clean\nyour content",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(42.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Record Video",
                iconRes = R.drawable.ic_record_video,
                c1 = Color(0xFF0D5C5A),
                c2 = Color(0xFF0C2125)
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Edit Content",
                iconRes = R.drawable.ic_edit_content,
                c1 = Color(0xFF3A1A70),
                c2 = Color(0xFF101228)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Add Effects",
                iconRes = R.drawable.ic_add_effects,
                c1 = Color(0xFF5C1010),
                c2 = Color(0xFF2A0909)
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Clean Feed",
                iconRes = R.drawable.ic_clean_feed,
                c1 = Color(0xFF5A3613),
                c2 = Color(0xFF4E121E)
            )
        }

        Spacer(modifier = Modifier.height(56.dp))
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(62.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
        ) {
            Text(
                text = "Enter Demo",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
