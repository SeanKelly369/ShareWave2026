package com.example.sharpwave2026.utils


private fun two(n: Long) = n.toString().padStart(2, '0')

fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60

    return if (h > 0) "${h}:${two(m)}:${two(s)}"
    else "${m}:${two(s)}"
}
