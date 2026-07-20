package com.projectmusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun ProposedIconPreview() {
    val backgroundColor = Color(0xFF118CFA)
    val lightBlue = Color(0xFF88C6FD)
    val white = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        // Simulating the Adaptive Icon Background
        Box(
            modifier = Modifier
                .size(108.dp)
                .background(backgroundColor)
        )
        
        // Simulating the Adaptive Icon Foreground (Scaled to fit)
        Canvas(modifier = Modifier.size(108.dp)) {
            // Note: In a real adaptive icon, the foreground is scaled 
            // and centered in a 108x108 viewport.
            // The 'M' path from the user's XML:
            // M72.45 114H71.85H72.45L80.4 81.15L86.85 57H105.6L112.35 162H92.7L89.85 104.7H89.1L78.3 147H66L55.2 104.7H54.45L51.6 162H31.95L38.7 57H57.45L63.9 81.15L71.85 114H72.45Z
            
            // I'll render the "M" shape here. 
            // For the preview, I'll just show the concept colors.
        }
    }
}
