package io.github.flassuu.talex

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TalexApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // SQLCipher ships a native library; load it once per process before any database opens.
        runCatching { System.loadLibrary("sqlcipher") }
    }
}
