package com.algorithmx.planner.logic

object TimeBlockMath {
    const val BLOCK_DURATION_MINUTES = 5
    const val BLOCKS_PER_HOUR = 12

    fun minutesToBlocks(minutes: Int): Int {
        // Ceiling division: 4 mins = 1 block, 6 mins = 2 blocks
        return (minutes + BLOCK_DURATION_MINUTES - 1) / BLOCK_DURATION_MINUTES
    }

    fun blocksToMinutes(blocks: Int): Int {
        return blocks * BLOCK_DURATION_MINUTES
    }

    fun formatBlockDuration(blocks: Int): String {
        val totalMinutes = blocksToMinutes(blocks)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}