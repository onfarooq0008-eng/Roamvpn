package com.roamvpn.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.roamvpn.R
import com.roamvpn.databinding.ActivityPremiumBinding
import com.roamvpn.databinding.ViewFeatureRowBinding
import com.roamvpn.util.applyEdgeToEdgeInsets

/**
 * "Upgrade to Premium" screen from the design.
 *
 * NOT REACHABLE YET: Roam VPN has no paid tier, billing or entitlement check, so every entry point to
 * this screen is hidden while BuildConfig.SHOW_PREMIUM is false (app/build.gradle). Before turning that
 * flag on you must (1) integrate Google Play Billing, (2) have the backend honour the purchase, and
 * (3) make the three feature lines below describe exactly what Premium really delivers.
 */
class PremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)

        feature(binding.premiumFeature1, R.drawable.ic_globe, "More locations", "Placeholder: describe the extra locations")
        feature(binding.premiumFeature2, R.drawable.ic_bolt, "Faster connections", "Placeholder: describe the speed benefit")
        feature(binding.premiumFeature3, R.drawable.ic_block, "No ads", "Placeholder: Premium removes the banner ad")

        binding.buttonPremiumBack.setOnClickListener { finish() }
        binding.buttonMaybeLater.setOnClickListener { finish() }
        binding.buttonGoPremium.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Premium is not available yet")
                .setMessage("Purchases have not been set up in this version of Roam VPN.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun feature(b: ViewFeatureRowBinding, icon: Int, title: String, body: String) {
        b.featureIcon.setImageResource(icon)
        b.featureTitle.text = title
        b.featureBody.text = body
    }
}
