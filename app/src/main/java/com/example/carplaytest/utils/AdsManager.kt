package com.example.carplaytest.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import com.example.carplaytest.R
import com.example.carplaytest.databinding.BannerAdViewBinding
import com.example.carplaytest.databinding.NativeMediumAdLayoutBinding
import com.example.carplaytest.databinding.NativeSmallAdLayoutBinding
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException

class AdsManager {
    private var interstitialAd: InterstitialAd? = null
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private lateinit var sessionManager: SessionManager

    companion object {
        private var mInstance: AdsManager? = null
        var showAd = true
        fun getInstance(context: Context): AdsManager {
            if (mInstance == null) {
                mInstance = AdsManager()
                mInstance!!.loadAd(context)
            }
            return mInstance!!
        }
    }

    fun loadAd(context: Context) {
        if (sessionManager.getBoolean(SessionManager.SessionKeys.IS_REMOVE_AD_PURCHASED, false)) {
            interstitialAd = null
            Log.d("adsManager", "Ad purchased")
            return
        }
        /*if (!SessionManager.getBool(SessionManager.IS_GDPR_CHECKED, false)) {
            interstitialAd = null
            Log.d("adsManager", "Block Ad due to GDPR")
            return
        }*/
        val adRequest = AdRequest.Builder().build()
        if (!remoteConfig.getValue("interstitial_ad").asBoolean()) {
            interstitialAd = null
            Log.d("adsManager", "Block Ad using remote Config")
            return
        }
        if (interstitialAd != null) {
            Log.d("adsManager", "Already loaded")
            return
        }
        InterstitialAd.load(context, context.getString(R.string.interstitial_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    this@AdsManager.interstitialAd = interstitialAd
                }
            })
    }

    fun showAd(activity: Activity, listener: IInterstitialListener) {
        if (interstitialAd != null) {
            if (showAd) {
                Handler(Looper.getMainLooper()).postDelayed({
                    interstitialAd!!.show(activity)
                    interstitialAd!!.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                listener.onError("ads dismiss")
                                interstitialAd = null
                                loadAd(activity.application)
                                showAd = false
                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                super.onAdFailedToShowFullScreenContent(p0)
                                listener.onError("failed to show ")
                                loadAd(activity.application)
                                showAd = true
                            }
                        }
                }, 900)
            } else {
                listener.onError("block to show")
                showAd = true
            }
        } else {
            loadAd(activity.applicationContext)
            listener.onError("ads not available ")
            showAd = true
        }
    }

    fun loadBanner(context: Context, listener: IBannerListener) {
        val binding = BannerAdViewBinding.inflate(LayoutInflater.from(context))
        if (!remoteConfig.getValue("banner_ad").asBoolean()) {
            listener.onBannerError("Ad Closed from Remote Config")
            return
        }
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.adListener = object : AdListener() {
            override fun onAdClicked() {}

            override fun onAdClosed() {}

            override fun onAdFailedToLoad(adError: LoadAdError) {
                listener.onBannerError("Failed to Load Message=> ${adError.message}")
            }

            override fun onAdImpression() {}

            override fun onAdLoaded() {
                listener.onBannerLoaded(binding.root)
            }

            override fun onAdOpened() {}
        }

    }

    fun loadCollapsibleBanner(context: Context, adSize: AdSize, listener: IBannerListener) {
        if (sessionManager.getBoolean(SessionManager.SessionKeys.IS_REMOVE_AD_PURCHASED, false)) {
            listener.onBannerError("Ad Purchased")
            Log.d("adsManager", "Ad purchased")
            return
        }
        if (!remoteConfig.getValue("banner_ad").asBoolean()) {
            listener.onBannerError("Ad Closed from Remote Config")
            return
        }

        val adView = AdView(context)
        adView.adUnitId = context.getString(R.string.banner_id)
        val extras = Bundle()
        extras.putString("collapsible", "bottom")
        adView.setAdSize(adSize);
        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
                listener.onBannerError("Failed to Load Message=> ${adError.message}")
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                listener.onBannerLoaded(adView)
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
        adView.loadAd(adRequest)


    }

    fun nativeLoadAd(context: Context, listener: NativeLoadCallback) {
        if (sessionManager.getBoolean(SessionManager.SessionKeys.IS_REMOVE_AD_PURCHASED, false)) {
            listener.onErrorAd("Ad purchased")
            Log.d("adsManager", "Ad purchased")
            return
        }
        /*if (!remoteConfig.getValue("native_ad").asBoolean()) {
            listener.onErrorAd("Blocked by Native ad")
            return
        }*/
        /*if (!SessionManager.getBool(SessionManager.IS_GDPR_CHECKED, false)) {
            Log.d("adsManager", "Block Ad due to GDPR")
            return
        }*/

        val binding = NativeSmallAdLayoutBinding.inflate(LayoutInflater.from(context))
        val adLoader = AdLoader.Builder(context, context.resources.getString(R.string.mrec_id))
            .forNativeAd {
                val style = NativeTemplateStyle.Builder().withMainBackgroundColor(
                    ColorDrawable(Color.WHITE)
                ).build()
                binding.myTemplate.setStyles(style)
                binding.myTemplate.setNativeAd(it)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    listener.onLoadAd(binding.root)
                }
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    listener.onErrorAd("Failed to load")
                }
            })
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 2)

    }

    fun nativeMediumLoadAd(context: Context, listener: NativeLoadCallback) {
        if (sessionManager.getBoolean(SessionManager.SessionKeys.IS_REMOVE_AD_PURCHASED, false)) {
            listener.onErrorAd("Ad purchased")
            Log.d("adsManager", "Ad purchased")
            return
        }
        /*if (!SessionManager.getBool(SessionManager.IS_GDPR_CHECKED, false)) {
            Log.d("adsManager", "Block Ad due to GDPR")
            return
        }*/
        if (!remoteConfig.getValue("native_ad").asBoolean()) {
            listener.onErrorAd("Blocked by Remote config")
            return
        }
        val binding = NativeMediumAdLayoutBinding.inflate(LayoutInflater.from(context))
        val adLoader = AdLoader.Builder(context, context.resources.getString(R.string.mrec_id))
            .forNativeAd {
                val style = NativeTemplateStyle.Builder().withMainBackgroundColor(
                    ColorDrawable(
                        Color.WHITE
                    )
                ).build()
                binding.myTemplate.setStyles(style)
                binding.myTemplate.setNativeAd(it)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    //binding.myTemplate.visibility = View.VISIBLE
                    listener.onLoadAd(binding.root)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    listener.onErrorAd("Failed to load")
                }
            }).build()
        adLoader.loadAds(AdRequest.Builder().build(), 2)
    }

    fun remoteConfigBannerUpdateListener(listener: OnAdsConfigChange) {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d("remote_config", "Updated keys: " + configUpdate.updatedKeys)
                remoteConfig.activate().addOnCompleteListener {
                    listener.onAdsChange(configUpdate.updatedKeys)
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w("TAG", "Config update error with code: " + error.code, error)
            }
        })
    }

    interface NativeLoadCallback {
        fun onLoadAd(root: TemplateView)
        fun onErrorAd(error: String)
    }

    interface OnAdsConfigChange {
        fun onAdsChange(config: MutableSet<String>)
    }

    interface IBannerListener {
        fun onBannerLoaded(root: AdView)
        fun onBannerError(s: String)
    }

    interface IInterstitialListener {
        fun onError(message: String)
        fun onAdLoaded(interstitialAd: InterstitialAd)
    }
}
