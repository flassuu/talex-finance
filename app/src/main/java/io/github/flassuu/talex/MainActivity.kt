package io.github.flassuu.talex

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.ui.navigation.TalexApp
import io.github.flassuu.talex.ui.theme.TalexTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var session: AppSessionManager

    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots and the recents preview for financial data.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            TalexTheme {
                TalexApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val leftAt = backgroundedAt
        if (leftAt != 0L && SystemClock.elapsedRealtime() - leftAt >= LOCK_TIMEOUT_MS) {
            session.lock()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    companion object {
        // Provisional relock policy: locking after a minute in the background.
        private const val LOCK_TIMEOUT_MS = 60_000L
    }
}
