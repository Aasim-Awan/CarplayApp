package com.example.carplaytest.utils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.carplaytest.databinding.ActivityOpenAdsBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback

class OpenAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenAdsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()


        if (AppController.openAppAd != null) {
            AppController.openAppAd!!.show(this)
            AppController.openAppAd!!.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        AppController.openAppAd = null
                        AppController.isShowingAd = false
                        AppController.appOpenAdManager.loadAd(this@OpenAdsActivity)
                        finish()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        super.onAdFailedToShowFullScreenContent(p0)
                        AppController.openAppAd = null
                        AppController.isShowingAd = false
                        AppController.appOpenAdManager.loadAd(this@OpenAdsActivity)
                        finish()
                    }

                }
        } else {
            AppController.openAppAd = null
            AppController.isShowingAd = false
            finish()
        }

    }
}