package com.algorithmx.planner.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TaskTreeConnector(
    connectorHeight: Dp, // Renamed from 'height' to avoid conflict with Canvas 'size.height'
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    isLast: Boolean = false
) {
    Canvas(modifier = Modifier.width(32.dp).height(connectorHeight)) {
        val strokeWidth = 2.dp.toPx()
        val midX = size.width / 2

        // Use 'size.height' which comes from the Canvas scope (pixels)
        val canvasHeight = size.height

        // Draw Vertical Line
        // If it's the last item, stop halfway down. Otherwise go full height.
        val endY = if (isLast) canvasHeight / 2 else canvasHeight

        drawLine(
            color = color,
            start = Offset(midX, 0f),
            end = Offset(midX, endY),
            strokeWidth = strokeWidth
        )

        // Draw Horizontal Line (The "L" shape)
        drawLine(
            color = color,
            start = Offset(midX, canvasHeight / 2),
            end = Offset(size.width, canvasHeight / 2),
            strokeWidth = strokeWidth
        )
    }
}