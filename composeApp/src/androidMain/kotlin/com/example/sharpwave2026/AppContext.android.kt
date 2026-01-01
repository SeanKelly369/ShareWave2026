package com.example.sharpwave2026

import android.content.Context

object AndroidAppContext {
    lateinit var app: Context
        private set

    fun init (context: Context) {
        app = context.applicationContext

    }
}
