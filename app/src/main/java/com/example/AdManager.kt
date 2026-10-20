package com.example

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // Real AdMob Ad Unit IDs provided by the user
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3785297962335569/2434118802"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3785297962335569/8210109531"

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownTime: Long = 0
    private const val MIN_INTERVAL_BETWEEN_ADS_MS = 60_000L // At least 60s cooldown

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "AdMob Interstitial loaded successfully.")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "AdMob Interstitial failed to load: ${loadAdError.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /**
     * Show interstitial ad respectfully:
     * - Only after enough cars delivered (e.g. every 3 deliveries)
     * - Respecting minimum cooldown so player is never interrupted mid-action
     */
    fun showInterstitialIfAppropriate(activity: Activity, doneCount: Int) {
        val now = System.currentTimeMillis()
        if (doneCount > 0 && doneCount % 3 == 0) {
            if (now - lastInterstitialShownTime >= MIN_INTERVAL_BETWEEN_ADS_MS) {
                interstitialAd?.let { ad ->
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial dismissed.")
                            interstitialAd = null
                            lastInterstitialShownTime = System.currentTimeMillis()
                            preloadInterstitial(activity)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                            interstitialAd = null
                            preloadInterstitial(activity)
                        }
                    }
                    ad.show(activity)
                } ?: run {
                    preloadInterstitial(activity)
                }
            }
        }
    }
}
