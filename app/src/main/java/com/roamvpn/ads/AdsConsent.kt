package com.roamvpn.ads

import android.content.Context
import com.roamvpn.data.AppSettings

/**
 * Ads are allowed automatically once the user has tapped "Agree & Continue" on the first-run consent screen
 * (ConsentActivity), which tells them in plain words that the app is supported by Yandex ads and links the
 * Privacy Policy and Terms. There is no separate ads dialog. Until that screen has been accepted, no ad
 * SDK is started and nothing is requested.
 */
object AdsConsent {
    fun isAccepted(context: Context): Boolean = AppSettings(context).hasAcceptedTerms
}
