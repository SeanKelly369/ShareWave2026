package com.example.sharpwave2026

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform