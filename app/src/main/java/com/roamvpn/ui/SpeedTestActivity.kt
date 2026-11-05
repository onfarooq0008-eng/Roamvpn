package com.roamvpn.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roamvpn.R
import com.roamvpn.databinding.ActivitySpeedTestBinding
import com.roamvpn.util.SpeedTest
import com.roamvpn.util.applyEdgeToEdgeInsets
import com.roamvpn.vpn.TunnelState
import com.roamvpn.vpn.VpnTunnelManagerHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/** Measures ping, download and upload over whatever connection the phone is using right now
 *  (through the VPN tunnel when it is connected). */
class SpeedTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeedTestBinding
    private var speedJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)

        binding.buttonSpeedBack.setOnClickListener { finish() }
        binding.buttonSpeedStart.setOnClickListener {
            if (speedJob?.isActive == true) speedJob?.cancel() else runSpeedTest()
        }
        binding.textSpeedServer.text =
            if (VpnTunnelManagerHolder.get(this).stateFlow.value == TunnelState.UP) "Through Roam VPN" else "Direct connection (VPN off)"

        binding.bottomNav.selectedItemId = R.id.nav_speed
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { openMain(MainActivity.TAB_HOME); true }
                R.id.nav_locations -> { openMain(MainActivity.TAB_LOCATIONS); true }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
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

    private fun setGauge(mbps: Double) {
        binding.speedGauge.setValue(mbps.toFloat())
        binding.textSpeedValue.text = String.format(Locale.US, "%.1f", mbps)
    }

    private fun runSpeedTest() {
        binding.buttonSpeedStart.text = "Stop"
        binding.textSpeedPing.text = "–"
        binding.textSpeedDownload.text = "–"
        binding.textSpeedUpload.text = "–"
        setGauge(0.0)
        speedJob = lifecycleScope.launch {
            try {
                binding.textSpeedPhase.text = "Checking ping…"
                binding.textSpeedPing.text = SpeedTest.ping().toString()

                binding.textSpeedPhase.text = "Testing download…"
                val down = SpeedTest.download { setGauge(it) }
                binding.textSpeedDownload.text = String.format(Locale.US, "%.1f", down)

                binding.textSpeedPhase.text = "Testing upload…"
                val up = SpeedTest.upload { setGauge(it) }
                binding.textSpeedUpload.text = String.format(Locale.US, "%.1f", up)

                setGauge(down)
                binding.textSpeedPhase.text = "Done"
            } catch (e: CancellationException) {
                binding.textSpeedPhase.text = "Stopped"
                throw e
            } catch (e: Exception) {
                binding.textSpeedPhase.text = "Test failed"
                Toast.makeText(this@SpeedTestActivity, "Speed test failed. Check your internet connection and try again.", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSpeedStart.text = "Test Again"
            }
        }
    }
}
