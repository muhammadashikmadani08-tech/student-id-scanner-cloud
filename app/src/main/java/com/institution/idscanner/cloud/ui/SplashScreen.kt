package com.institution.idscanner.cloud.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

private val SplashGreen = Color(0xFF0B3D2E)
private val SplashGold = Color(0xFFF5B301)
private val SplashCream = Color(0xFFFAF6E8)
private val SplashMuted = Color(0xFFBECDc3)

/**
 * Welcome screen shown first on every cold start (see MainActivity):
 * institution logo, institution name, app subtitle, and the developer
 * credit at the bottom. [logoPath] is an absolute internal-storage path
 * from LogoStore, or null to use the default scan icon.
 */
@Composable
fun SplashScreen(institutionName: String, logoPath: String?) {
    val alpha by animateFloatAsState(
        targetValue = 1f, animationSpec = tween(durationMillis = 600), label = "splashFade"
    )
    Box(
        Modifier.fillMaxSize().background(SplashGreen).alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo: saved logo image in a gold ring, else the default icon.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .border(4.dp, SplashGold, CircleShape)
                    .padding(10.dp)
            ) {
                if (!logoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(logoPath), contentDescription = "প্রতিষ্ঠানের লোগো",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.QrCodeScanner, contentDescription = null,
                        tint = SplashGold, modifier = Modifier.size(96.dp)
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(
                institutionName.ifBlank { "আপনার প্রতিষ্ঠানের নাম" },
                color = SplashCream, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, lineHeight = 40.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "স্টুডেন্ট আইডি স্ক্যানার",
                color = SplashGold, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.size(width = 64.dp, height = 4.dp).background(SplashGold)
            )
        }
        // Developer credit, pinned to the bottom.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp)
        ) {
            Text("Developed by", color = SplashMuted, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Muhammed Ashik Madani",
                color = SplashGold, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
