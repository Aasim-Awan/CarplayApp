package com.example.carplaytest.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.carplaytest.R
import com.example.carplaytest.main.MainActivity
import com.example.carplaytest.utils.SessionManager.SessionKeys
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class AppController: Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var isLoadingAd = false
    private var currentActivity: Activity? = null
    private val TAG = AppController::class.java.simpleName
    private lateinit var sesssionManager: SessionManager

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    override fun onCreate() {
        super.onCreate()
        sesssionManager = SessionManager(this)

    }

    companion object {
        lateinit var appOpenAdManager: AppOpenAdManager
        public var openAppAd: AppOpenAd? = null
        var isShowingAd = false
        var context: Context? = null
    }

    inner class AppOpenAdManager {
        fun loadAd(context: Context) {

            if (sesssionManager.getBoolean(SessionKeys.IS_REMOVE_AD_PURCHASED, false)) {
                Log.d("adsManager", "Ad purchased")
                return
            }

            if (!sesssionManager.getBoolean(SessionKeys.IS_GDPR_CHECKED, false)){
                return
            }
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            if (!remoteConfig.getValue("app_open_ad").asBoolean()){
                Log.d("AD_LOG", "remote config false")
                return
            }
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, context.resources.getString(R.string.app_open), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d("AD_LOG", "Ad was loaded.")
                        openAppAd = ad
                        openAppAd = ad
                        isLoadingAd = false
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d("AD_LOG", loadAdError.message)
                        isLoadingAd = false;
                    }
                })
        }

        fun showAdIfAvailable(activity: Activity, listener: OnShowAdCompleteListener) {
            if (isShowingAd) {
                Log.d("AD_LOG", "The app open ad is already showing.")
                return
            }

            if (!isAdAvailable()) {
                Log.d("AD_LOG", "The app open ad is not ready yet.")
                listener.onShowAdComplete()
                loadAd(activity)
                return
            }

            openAppAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    openAppAd = null
                    isShowingAd = false
                    val cAct = activity.javaClass.simpleName
                    if (cAct == "SplashActivity"){
                        startActivity(
                            Intent(activity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        activity.finish()
                    }

                    listener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AD_LOG", adError.message)
                    openAppAd = null
                    isShowingAd = false

                    listener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d("AD_LOG", "Ad showed fullscreen content.")
                }
            }

            isShowingAd = true
            startActivity(
                Intent(activity, OpenAdsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )

        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        currentActivity?.let {
            appOpenAdManager.showAdIfAvailable(it, object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    // Empty because the user will go back to the activity that shows the ad.
                }
            })
        }
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        return openAppAd != null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

}