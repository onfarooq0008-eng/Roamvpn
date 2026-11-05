package com.roamvpn.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.roamvpn.databinding.ActivityWelcomeBinding
import com.roamvpn.util.applyEdgeToEdgeInsets

/** First-run welcome screen ("Get Started"). Leads to the privacy/consent screen. */
class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.welcomeContent)
        binding.buttonGetStarted.setOnClickListener {
            startActivity(Intent(this, ConsentActivity::class.java))
        }
    }
}
