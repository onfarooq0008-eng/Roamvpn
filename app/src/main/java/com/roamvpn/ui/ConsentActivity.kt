package com.roamvpn.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.roamvpn.R
import com.roamvpn.data.AppSettings
import com.roamvpn.data.ServerCache
import com.roamvpn.databinding.ActivityConsentBinding
import com.roamvpn.databinding.ViewFeatureRowBinding
import com.roamvpn.util.applyEdgeToEdgeInsets

/**
 * "Your Privacy Matters": shown once, before MainActivity is ever reached, on first launch. Gates
 * access behind explicit acceptance of the Privacy Policy / Terms -- required for Play Store review of
 * any VPN app (BIND_VPN_SERVICE is a sensitive permission). The policy text itself lives in
 * backend/api/public/privacy.html and terms.html, served from AppSettings.backendApiUrl, the same
 * host SettingsActivity's legal links use.
 *
 * The close (X) button declines -- since using the app at all requires accepting how it handles your
 * traffic, declining just closes the app.
 */
class ConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.consentContent)

        feature(binding.featureEncrypt, R.drawable.ic_lock, "Encrypts your data", "Traffic is encrypted with WireGuard")
        feature(binding.featureIp, R.drawable.ic_visibility_off, "Hides your IP address", "Websites see the VPN server's IP, not yours")
        feature(binding.featureLogs, R.drawable.ic_doc, "No browsing logs", "We don't store your browsing activity")
        feature(binding.featureVerified, R.drawable.ic_shield_check, "Verified connection", "We confirm traffic flows before showing Connected")

        val siteBaseUrl = AppSettings(this).backendApiUrl.trimEnd('/')
        binding.linkPrivacy.setOnClickListener { openLink("$siteBaseUrl/privacy.html") }
        binding.linkTerms.setOnClickListener { openLink("$siteBaseUrl/terms.html") }

        binding.buttonClose.setOnClickListener { finishAffinity() }

        binding.buttonAgree.setOnClickListener {
            AppSettings(this).hasAcceptedTerms = true
            com.roamvpn.ads.AdsManager.init(application) // ads are allowed by this Agree tap (the text above says so)
            // This is the one path to MainActivity that doesn't go through SplashActivity, so it must
            // start the server prefetch itself -- otherwise a brand-new user's first launch would skip it.
            ServerCache.warm(this)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }

    private fun feature(b: ViewFeatureRowBinding, icon: Int, title: String, body: String) {
        b.featureIcon.setImageResource(icon)
        b.featureTitle.text = title
        b.featureBody.text = body
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser app found to open this link", Toast.LENGTH_SHORT).show()
        }
    }
}
