package com.swamisachidanand;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Interstitial ad: audio book open, YouTube video open, max once per 5 min.
 */
public class InterstitialAdHelper {

    private static final String TAG = "InterstitialAdHelper";
    private static final String L = "INTERSTITIAL";
    private static final String PREFS = "interstitial_prefs";
    private static final String KEY_LAST_SHOWN = "last_shown_ms";
    private static final long THROTTLE_MS = 5 * 60 * 1000; // 5 min

    private static InterstitialAd sInterstitialAd;
    private static boolean sLoading;

    public static void preload(android.content.Context context) {
        if (!BuildConfig.ADS_ENABLED) {
            return;
        }
        if (context == null || sInterstitialAd != null || sLoading) {
            AdLog.d(L, "preload skip (ctx=" + (context != null) + " hasAd=" + (sInterstitialAd != null) + " loading=" + sLoading + ")");
            return;
        }
        sLoading = true;
        String unitId = context.getString(R.string.admob_interstitial_unit_id);
        AdLog.d(L, "preload start unitId=" + unitId);
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(
                context.getApplicationContext(),
                unitId,
                request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        sLoading = false;
                        sInterstitialAd = ad;
                        Log.d(TAG, "Interstitial preloaded");
                        AdLog.i(L, "preload onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        sLoading = false;
                        Log.w(TAG, "Interstitial load failed: " + loadAdError.getMessage());
                        AdLog.w(L, "preload onAdFailedToLoad " + AdLog.formatLoadError(loadAdError));
                    }
                });
    }

    /**
     * Show interstitial if allowed (5 min throttle). Runs onDone when ad closed or skipped.
     */
    public static void showIfAllowed(Activity activity, Runnable onDone) {
        if (activity == null || activity.isFinishing()) {
            AdLog.d(L, "showIfAllowed skip (no activity)");
            if (onDone != null) onDone.run();
            return;
        }
        if (!BuildConfig.ADS_ENABLED) {
            if (onDone != null) onDone.run();
            return;
        }
        SharedPreferences sp = activity.getSharedPreferences(PREFS, 0);
        long last = sp.getLong(KEY_LAST_SHOWN, 0);
        long now = System.currentTimeMillis();
        if (now - last < THROTTLE_MS) {
            AdLog.d(L, "showIfAllowed throttled (" + ((now - last) / 1000) + "s since last, need " + (THROTTLE_MS / 1000) + "s)");
            if (onDone != null) onDone.run();
            return;
        }
        InterstitialAd ad = sInterstitialAd;
        if (ad == null) {
            AdLog.d(L, "showIfAllowed no cached ad — load then show");
            // Ad not ready yet — load now then show (first tap was skipping ad entirely)
            loadAndShowInterstitial(activity, sp, onDone);
            return;
        }
        AdLog.i(L, "showIfAllowed showing cached ad");
        sInterstitialAd = null;
        preload(activity);
        attachShowCallbacksAndShow(activity, sp, ad, onDone);
    }

    private static void loadAndShowInterstitial(Activity activity, SharedPreferences sp, Runnable onDone) {
        if (activity == null || activity.isFinishing()) {
            if (onDone != null) onDone.run();
            return;
        }
        String unitId = activity.getString(R.string.admob_interstitial_unit_id);
        AdLog.d(L, "loadAndShowInterstitial unitId=" + unitId);
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(
                activity.getApplicationContext(),
                unitId,
                request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        AdLog.i(L, "load-for-show onAdLoaded");
                        activity.runOnUiThread(() -> {
                            if (activity.isFinishing()) {
                                if (onDone != null) onDone.run();
                                return;
                            }
                            attachShowCallbacksAndShow(activity, sp, ad, onDone);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Interstitial load-for-show failed: " + loadAdError.getMessage());
                        AdLog.w(L, "load-for-show onAdFailedToLoad " + AdLog.formatLoadError(loadAdError));
                        try {
                            if (activity != null && !activity.isFinishing()) {
                                activity.runOnUiThread(() -> {
                                    if (onDone != null) onDone.run();
                                    preload(activity);
                                });
                            } else {
                                if (onDone != null) onDone.run();
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "runOnUiThread failed", t);
                            if (onDone != null) onDone.run();
                        }
                    }
                });
    }

    private static void attachShowCallbacksAndShow(Activity activity, SharedPreferences sp, InterstitialAd ad, Runnable onDone) {
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                AdLog.d(L, "onAdDismissedFullScreenContent");
                AdVolumeHelper.restoreAfterAd(activity);
                sp.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply();
                preload(activity);
                if (onDone != null) activity.runOnUiThread(onDone);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                Log.w(TAG, "Interstitial show failed: " + adError.getMessage());
                AdLog.w(L, "onAdFailedToShowFullScreenContent code=" + adError.getCode() + " msg=" + adError.getMessage());
                preload(activity);
                if (onDone != null) activity.runOnUiThread(onDone);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                AdLog.i(L, "onAdShowedFullScreenContent");
                AdVolumeHelper.muteForAd(activity);
            }
        });
        AdLog.d(L, "ad.show()");
        ad.show(activity);
    }
}
