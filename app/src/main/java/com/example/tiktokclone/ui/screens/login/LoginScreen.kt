package com.example.tiktokclone.ui.screens.login

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiktokclone.R
import com.example.tiktokclone.ui.theme.Pink40

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF2C2C2C),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = "Help",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            Text(
                text = "Log in to TikTok",
                color = Color(0xFF1F1F1F),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "UI-only demo. No backend, no authorization.",
                color = Color(0xFF8F8F8F),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LoginMethodRow(iconRes = R.drawable.ic_phone, text = "Use phone / email / username")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_facebook, text = "Continue with Facebook")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_apple, text = "Continue with Apple")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_google, text = "Continue with Google")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_x, text = "Continue with X")
            Spacer(modifier = Modifier.height(10.dp))
            LoginMethodRow(iconRes = R.drawable.ic_instagram, text = "Continue with Instagram")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63))
            ) {
                Text("Enter Demo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(style = SpanStyle(color = Pink40, fontWeight = FontWeight.SemiBold)) {
                        append("Sign up")
                    }
                },
                color = Color(0xFF8A8A8A),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoginMethodRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(0.dp))
            .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(0.dp))
            .clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(14.dp))
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            color = Color(0xFF2C2C2C),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

