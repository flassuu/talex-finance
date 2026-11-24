package io.github.flassuu.talex

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.flassuu.talex.core.locale.LanguagePreferences
import io.github.flassuu.talex.core.locale.withAppLanguage
import io.github.flassuu.talex.core.security.LockTimeout
import io.github.flassuu.talex.data.security.SecurityPreferences
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.ui.navigation.TalexApp
import io.github.flassuu.talex.ui.theme.TalexTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var session: AppSessionManager

    private var backgroundedAt = 0L

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage(LanguagePreferences.get(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots and the recents preview for financial data in release
        // builds. Debug builds allow screen capture so the app can be tested with
        // scrcpy; never ship a debuggable build.
        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            TalexTheme {
                TalexApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val timeout = SecurityPreferences.lockTimeout(this)
        if (timeout == LockTimeout.IMMEDIATELY) return
        val leftAt = backgroundedAt
        if (leftAt != 0L && SystemClock.elapsedRealtime() - leftAt >= timeout.millis) {
            session.lock()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
        // "Immediately" means the database closes as soon as the app is hidden.
        if (SecurityPreferences.lockTimeout(this) == LockTimeout.IMMEDIATELY) {
            session.lock()
        }
    }
}
