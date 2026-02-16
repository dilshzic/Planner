package com.algorithmx.planner.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.planner.logic.TimeBlockMath

@Composable
fun TimeBlockGrid(
    totalBlocks: Int = 192, // 16 Hours (e.g. 06:00 to 22:00)
    spentBlocks: Int,       // Blocks already done today
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Legend / Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Time Wallet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${TimeBlockMath.blocksToMinutes(totalBlocks - spentBlocks)}m Left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // The Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(12), // 12 Blocks = 1 Hour
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(300.dp) // Fixed height for scrolling inside
        ) {
            items(totalBlocks) { index ->
                val color = when {
                    index < spentBlocks -> Color(0xFF4CAF50) // Green (Spent)
                    else -> Color(0xFFE0E0E0) // Gray (Empty)
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(color, shape = RoundedCornerShape(2.dp))
                )
            }
        }
        
        // Helper text below grid
        Text(
            text = "Each square = 5 mins. 1 Row = 1 Hour.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}