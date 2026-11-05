package com.roamvpn.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.roamvpn.BuildConfig
import com.roamvpn.R
import com.roamvpn.data.AppSettings
import com.roamvpn.databinding.ActivitySettingsBinding
import com.roamvpn.databinding.ViewSettingRowBinding
import com.roamvpn.util.NotificationHelper
import com.roamvpn.util.applyEdgeToEdgeInsets

/** Regular-user settings only. Server management is handled entirely by the
 *  backend/VPS, so there's no in-app admin surface here. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    // Turning "Notifications" on needs the Android 13+ runtime permission; if it is refused the switch goes back off.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            settings.notificationsEnabled = true
        } else {
            settings.notificationsEnabled = false
            binding.rowNotifications.rowSwitch.isChecked = false
            Toast.makeText(this, "Allow notifications for Roam VPN in Android settings to turn this on", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)
        settings = AppSettings(this)

        binding.buttonSettingsBack.setOnClickListener { finish() }
        binding.textVersion.text = "Version ${BuildConfig.VERSION_NAME}"
        setUpBottomNav()
        setUpGeneral()
        setUpOther()
    }

    override fun onResume() {
        super.onResume()
        // DNS may have been changed from the dialog; keep the subtitle current.
        binding.rowDns.rowSubtitle.text = dnsLabel()
    }

    private fun setUpBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_settings
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { openMain(MainActivity.TAB_HOME); true }
                R.id.nav_locations -> { openMain(MainActivity.TAB_LOCATIONS); true }
                R.id.nav_speed -> {
                    startActivity(Intent(this, SpeedTestActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }

    private fun openMain(tab: Int) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun row(
        b: ViewSettingRowBinding,
        icon: Int,
        title: String,
        subtitle: String,
        withSwitch: Boolean = false,
        onClick: () -> Unit
    ) {
        b.rowIcon.setImageResource(icon)
        b.rowTitle.text = title
        b.rowSubtitle.text = subtitle
        b.rowSubtitle.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE
        b.rowSwitch.visibility = if (withSwitch) View.VISIBLE else View.GONE
        b.rowChevron.visibility = if (withSwitch) View.GONE else View.VISIBLE
        b.root.setOnClickListener { onClick() }
    }

    private fun setUpGeneral() {
        row(binding.rowAutoConnect, R.drawable.ic_refresh, "Auto Connect", "Connect on app launch", withSwitch = true) {
            binding.rowAutoConnect.rowSwitch.toggle()
        }
        binding.rowAutoConnect.rowSwitch.isChecked = settings.autoConnectEnabled
        binding.rowAutoConnect.rowSwitch.setOnCheckedChangeListener { _, checked ->
            settings.autoConnectEnabled = checked
        }

        row(binding.rowNotifications, R.drawable.ic_bell, "Notifications", "Get connection alerts", withSwitch = true) {
            binding.rowNotifications.rowSwitch.toggle()
        }
        binding.rowNotifications.rowSwitch.isChecked =
            settings.notificationsEnabled && NotificationHelper.hasNotificationPermission(this)
        binding.rowNotifications.rowSwitch.setOnCheckedChangeListener { _, checked ->
            if (!checked) {
                settings.notificationsEnabled = false
                NotificationHelper.clear(this)
            } else if (NotificationHelper.hasNotificationPermission(this)) {
                settings.notificationsEnabled = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        row(binding.rowProtocol, R.drawable.ic_protocol, "Connection Protocol", "WireGuard") {
            AlertDialog.Builder(this)
                .setTitle("Connection Protocol")
                .setMessage("Roam VPN uses WireGuard, a modern and fast VPN protocol. There is nothing to configure.")
                .setPositiveButton("OK", null)
                .show()
        }

        row(binding.rowKillSwitch, R.drawable.ic_block, "Kill Switch", "Block internet if VPN disconnects") {
            Toast.makeText(this, "Turn on “Block connections without VPN” for Roam VPN", Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Open Android Settings → Network → VPN", Toast.LENGTH_LONG).show()
            }
        }

        row(binding.rowSplit, R.drawable.ic_split, "Split Tunneling", "Choose apps to use VPN") {
            startActivity(Intent(this, SplitTunnelActivity::class.java))
        }

        row(binding.rowDns, R.drawable.ic_dns, "DNS", dnsLabel()) { showDnsDialog() }
        row(binding.rowClearLogs, R.drawable.ic_clear_logs, "Clear Logs", "Delete the saved server list and temporary data") {
            AlertDialog.Builder(this)
                .setTitle("Clear logs?")
                .setMessage("This deletes the saved server list and other temporary data stored on this phone. Roam VPN does not keep a record of your browsing.")
                .setPositiveButton("Clear") { _, _ ->
                    com.roamvpn.data.ServerCache.clear(this)
                    Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setUpOther() {
        val siteBaseUrl = settings.backendApiUrl.trimEnd('/')

        // Both open the Play Store listing (R.string.play_store_url).
        row(binding.rowRate, R.drawable.ic_star_border, "Rate Us", "If you like our app") {
            openInBrowser(getString(R.string.play_store_url))
        }
        row(binding.rowShare, R.drawable.ic_share, "Share App", "Share with your friends") {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Try Roam VPN, a WireGuard VPN for Android: ${getString(R.string.play_store_url)}")
            }
            startActivity(Intent.createChooser(send, "Share Roam VPN"))
        }
        row(binding.rowHelp, R.drawable.ic_help, "Help & Support", "Get help when you need it") { openContactEmail() }
        row(binding.rowPrivacy, R.drawable.ic_lock, "Privacy Policy", "") { openInBrowser("$siteBaseUrl/privacy.html") }
        row(binding.rowTerms, R.drawable.ic_doc, "Terms of Service", "") { openInBrowser("$siteBaseUrl/terms.html") }
        row(binding.rowAbout, R.drawable.ic_info, "About", "Version ${BuildConfig.VERSION_NAME}") {
            AlertDialog.Builder(this)
                .setTitle("Roam VPN")
                .setMessage("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n\nContact: ${getString(R.string.contact_email)}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun openInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser app found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openContactEmail() {
        val email = getString(R.string.contact_email)
        // ACTION_SENDTO with a mailto: Uri targets only email apps.
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Roam VPN Support")
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found — reach us at $email", Toast.LENGTH_LONG).show()
        }
    }

    // ---- DNS ----------------------------------------------------------------

    private val dnsModes = listOf("server", "google", "cloudflare", "adblock", "custom")
    private val dnsNames = listOf(
        "Server default", "Google  •  8.8.8.8", "Cloudflare  •  1.1.1.1", "AdGuard  •  Ad blocking", "Custom…"
    )

    private fun dnsLabel(): String = when (settings.dnsMode) {
        "google" -> "Google"
        "cloudflare" -> "Cloudflare"
        "adblock" -> "AdGuard (ad blocking)"
        "custom" -> "Custom"
        else -> "Server default"
    }

    /** See AppSettings.resolveDns / settingsChangePendingReconnect for how this is applied. */
    private fun showDnsDialog() {
        val checked = dnsModes.indexOf(settings.dnsMode).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("DNS resolver")
            .setSingleChoiceItems(dnsNames.toTypedArray(), checked) { dialog, which ->
                dialog.dismiss()
                val mode = dnsModes[which]
                if (mode == "custom") {
                    showCustomDnsDialog()
                } else {
                    settings.dnsMode = mode
                    settings.settingsChangePendingReconnect = true
                    binding.rowDns.rowSubtitle.text = dnsLabel()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomDnsDialog() {
        val input = EditText(this).apply {
            hint = "9.9.9.9, 149.112.112.112"
            setText(settings.customDns)
            setSingleLine()
        }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Custom DNS")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                settings.customDns = input.text.toString().trim()
                settings.dnsMode = "custom"
                settings.settingsChangePendingReconnect = true
                binding.rowDns.rowSubtitle.text = dnsLabel()
                Toast.makeText(this, "Custom DNS saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
