package com.example.candy.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "CandyAdManager"

    // 1. Real User Banner Ad Unit ID
    const val REAL_BANNER_AD_UNIT_ID = "ca-app-pub-1495262574338316/2803163788"

    // Official Google Sample Banner Ad Unit ID (used as reliable fallback if real ID has no fill or account is pending)
    const val FALLBACK_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    // 2-8. Real User Interstitial Ad Units ("Anuncios emergentes automáticos")
    val REAL_INTERSTITIAL_AD_UNIT_IDS = listOf(
        "ca-app-pub-1495262574338316/2903409205",
        "ca-app-pub-1495262574338316/2452733430",
        "ca-app-pub-1495262574338316/8970040322",
        "ca-app-pub-1495262574338316/1182179137",
        "ca-app-pub-1495262574338316/4962596335",
        "ca-app-pub-1495262574338316/2714752655",
        "ca-app-pub-1495262574338316/3503687777"
    )

    // Official Google Sample Interstitial Ad Unit ID (fallback)
    const val FALLBACK_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialIndex = 0
    private var currentInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var lastAdShowTime = 0L
    private const val MIN_COOLDOWN_MS = 6_000L // 6s fast cooldown so user can test and experience ads

    var bannerStatusText by mutableStateOf("AdMob Conectado")
        private set

    fun initialize(context: Context) {
        try {
            // Enable emulator test device mode so Google's server fulfills requests for developer testing
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google Mobile Ads initialized: $status")
                bannerStatusText = "AdMob Inicializado"
                loadNextInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob init error: ${e.message}")
        }
    }

    fun loadNextInterstitial(context: Context, useFallbackIfFailed: Boolean = true) {
        if (isAdLoading || currentInterstitialAd != null) return
        isAdLoading = true

        val adUnitId = REAL_INTERSTITIAL_AD_UNIT_IDS[interstitialIndex % REAL_INTERSTITIAL_AD_UNIT_IDS.size]
        interstitialIndex++

        Log.d(TAG, "Loading Interstitial Ad: $adUnitId")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Real Interstitial loaded successfully: $adUnitId")
                    currentInterstitialAd = interstitialAd
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Real Interstitial ($adUnitId) failed: ${loadAdError.message} (Code: ${loadAdError.code})")
                    isAdLoading = false
                    currentInterstitialAd = null

                    if (useFallbackIfFailed) {
                        // Fallback to Google sample interstitial so the ad unit is guaranteed to be available
                        loadFallbackInterstitial(context)
                    }
                }
            }
        )
    }

    private fun loadFallbackInterstitial(context: Context) {
        if (isAdLoading || currentInterstitialAd != null) return
        isAdLoading = true

        Log.d(TAG, "Loading fallback Google Interstitial Ad: $FALLBACK_INTERSTITIAL_AD_UNIT_ID")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            FALLBACK_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Fallback Interstitial loaded successfully")
                    currentInterstitialAd = interstitialAd
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Fallback Interstitial failed: ${loadAdError.message}")
                    isAdLoading = false
                    currentInterstitialAd = null
                }
            }
        )
    }

    /**
     * Shows automatic interstitial ad with callback
     */
    fun showAutomaticInterstitial(activity: Activity, onClosed: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = currentInterstitialAd

        if (ad != null && (now - lastAdShowTime) >= MIN_COOLDOWN_MS) {
            try {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed by user")
                        currentInterstitialAd = null
                        lastAdShowTime = System.currentTimeMillis()
                        loadNextInterstitial(activity)
                        onClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                        currentInterstitialAd = null
                        loadNextInterstitial(activity)
                        onClosed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad is now showing")
                    }
                }
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing interstitial: ${e.message}")
                currentInterstitialAd = null
                loadNextInterstitial(activity)
                onClosed()
            }
        } else {
            // Preload for next trigger and continue immediately
            loadNextInterstitial(activity)
            onClosed()
        }
    }
}

/**
 * Bottom Banner Ad Composable ("Sdk Google admob banner debajo")
 * Loads the user's real banner first; if Google has no fill yet, falls back to Google's live sample banner.
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AdMob Banner Preview", fontSize = 12.sp, color = Color.Gray)
        }
        return
    }

    var isLoaded by remember { mutableStateOf(false) }
    var currentAdUnit by remember { mutableStateOf(AdManager.REAL_BANNER_AD_UNIT_ID) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF140718)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtle AdMob Header Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Ad",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0xFFFFB300), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Text(
                    text = "Google AdMob",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Text(
                text = if (isLoaded) "En vivo • ca-app-pub-1495262574338316" else "Cargando anuncio...",
                fontSize = 9.sp,
                color = if (isLoaded) Color(0xFF00E676) else Color(0xFFFFD54F)
            )
        }

        // The AdView itself with key(currentAdUnit) to avoid IllegalStateException: The ad unit ID can only be set once on an AdView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF4081)
                    )
                    Text(
                        text = "Conectando con Google AdMob...",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            key(currentAdUnit) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = currentAdUnit
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    Log.d("CandyAdManager", "Banner ad loaded successfully from: $currentAdUnit")
                                    isLoaded = true
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    Log.w("CandyAdManager", "Banner ad failed on $currentAdUnit: ${error.message} (Code: ${error.code})")
                                    if (currentAdUnit != AdManager.FALLBACK_BANNER_AD_UNIT_ID) {
                                        currentAdUnit = AdManager.FALLBACK_BANNER_AD_UNIT_ID
                                    }
                                }
                            }
                            try {
                                loadAd(AdRequest.Builder().build())
                            } catch (e: Exception) {
                                Log.e("CandyAdManager", "Error loading banner: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}
