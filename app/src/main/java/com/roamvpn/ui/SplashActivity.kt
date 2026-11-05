package com.roamvpn.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.roamvpn.data.AppSettings
import com.roamvpn.data.ServerCache
import com.roamvpn.databinding.ActivitySplashBinding
import com.roamvpn.util.applyEdgeToEdgeInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Branded launch screen. Shows for a moment (while the server list is fetched in the background), then
 *  routes to the home screen, or to the welcome/privacy flow on first launch. */
class SplashActivity : AppCompatActivity() {

    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.splashContent)
        binding.splashProgress.setProgressCompat(100, true)

        val hasAcceptedTerms = AppSettings(this).hasAcceptedTerms
        // Start fetching servers while the logo is on screen -- but only once the terms are accepted:
        // no network calls before consent.
        if (hasAcceptedTerms) ServerCache.warm(this)

        lifecycleScope.launch {
            // Only counts down while the screen is visible, so we never start an activity from the background.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(SPLASH_MS)
                if (!navigated) {
                    navigated = true
                    val next = if (hasAcceptedTerms) MainActivity::class.java else WelcomeActivity::class.java
                    startActivity(Intent(this@SplashActivity, next))
                    finish()
                }
            }
        }
    }

    private companion object {
        const val SPLASH_MS = 1_000L
    }
}
