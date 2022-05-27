package com.forem.android.application

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import com.forem.android.BuildConfig
import dagger.hilt.android.HiltAndroidApp

/** The root [Application] of Forem app. */
@HiltAndroidApp
class ForemApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }
    }
}
