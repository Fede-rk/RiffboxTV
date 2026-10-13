package com.riffbox.tv

import android.app.Application
import com.riffbox.tv.security.SecurityGuard

class RiffboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Execute anti-tampering and security checks at startup
        SecurityGuard.initialize(this)
    }
}