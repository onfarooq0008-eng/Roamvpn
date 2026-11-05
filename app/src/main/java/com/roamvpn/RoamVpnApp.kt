package com.roamvpn

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.roamvpn.ads.AdsManager
import com.roamvpn.data.AppSettings

class RoamVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The Roam VPN design is dark-only, so the app never follows the system light theme.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        // Ads must not load before the user has accepted the consent screen (ConsentActivity). Returning
        // users get the ads SDK started here; first-run users get it from ConsentActivity's "Agree" button.
        // init() itself also waits for the user's "Allow ads" answer.
        if (AppSettings(this).hasAcceptedTerms) AdsManager.init(this)
    }
}
