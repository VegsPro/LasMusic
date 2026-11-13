package com.vegspro.lasmusic

import android.app.Application
import android.util.Log

class LasMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("LasMusicApp", "Application onCreate")
    }
}
