package com.projectmusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.R

@Preview(showBackground = true)
@Composable
fun IconOutputPreview() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // This is how it looks in your XML (The full vector)
        Image(
            painter = painterResource(id = R.drawable.temp_icon_test),
            contentDescription = null,
            modifier = Modifier.size(145.dp, 210.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdaptiveIconOutputPreview() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // This is how I will adapt it (Background + Scaled Foreground)
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape) // Standard mask
                .background(Color(0xFF118CFA)),
            contentAlignment = Alignment.Center
        ) {
            // I'll take just the "M" part for the foreground
            // To show you now, I'll just render the whole thing clipped
            Image(
                painter = painterResource(id = R.drawable.temp_icon_test),
                contentDescription = null,
                modifier = Modifier.size(72.dp) // Scaled to safe zone
            )
        }
    }
}
