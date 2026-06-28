package com.faqxd.livesub.android

import android.app.Application

/**
 * Application entry point. Holds process-wide singletons.
 *
 * Mirrors the Windows `main.py` `QApplication` setup; we keep this lightweight
 * because all real work lives in [com.faqxd.livesub.android.service.LiveTranslateService].
 */
class LiveBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        lateinit var instance: LiveBuddyApp
            private set
    }
}
