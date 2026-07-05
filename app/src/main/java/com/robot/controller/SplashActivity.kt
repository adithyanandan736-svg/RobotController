package com.robot.controller

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<android.widget.ImageView>(R.id.imgSplashLogo)
        val title = findViewById<android.widget.TextView>(R.id.txtSplashTitle)
        val tagline = findViewById<android.widget.TextView>(R.id.txtSplashTagline)

        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim)
        logo.startAnimation(logoAnim)

        title.animate().alpha(1f).setStartDelay(400).setDuration(500).start()
        tagline.animate().alpha(1f).setStartDelay(600).setDuration(500).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1800)
    }
}
