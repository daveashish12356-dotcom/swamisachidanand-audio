package com.swamisachidanand;

import android.content.Context;
import android.view.View;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

/**
 * Banner ads must load after {@link MobileAds} finishes initializing. Interstitials are preloaded
 * from {@link App} inside the init callback, but fragments were calling {@link AdView#loadAd}
 * immediately — often before the SDK was ready, so banners never filled while other formats worked.
 * Also applies anchored adaptive size for better fill on real devices.
 */
public final class BannerAdHelper {
    private static final String TAG = "BannerAdHelper";

    private BannerAdHelper() {}

    public static void setAdaptiveBannerSize(@NonNull AdView adView) {
        try {
            Context ctx = adView.getContext();
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return;
            DisplayMetrics out = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(out);
            float density = out.density;
            if (density <= 0f) density = 1f;
            int widthDp = (int) (out.widthPixels / density);
            if (widthDp < 320) widthDp = 320;
            AdSize adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp);
            adView.setAdSize(adaptive);
        } catch (Throwable t) {
            Log.w(TAG, "setAdaptiveBannerSize", t);
        }
    }

    /**
     * Runs {@link MobileAds#initialize} then {@link AdView#loadAd} on the AdView's thread.
     */
    public static void loadWhenReady(@Nullable Context context, @Nullable AdView adView, @NonNull AdRequest request) {
        if (context == null || adView == null) return;
        if (!BuildConfig.ADS_ENABLED) {
            try {
                adView.setVisibility(View.GONE);
            } catch (Throwable ignored) {
            }
            return;
        }
        final Context app = context.getApplicationContext();
        try {
            setAdaptiveBannerSize(adView);
        } catch (Throwable ignored) {
        }
        try {
            MobileAds.initialize(app, initializationStatus -> adView.post(() -> {
                try {
                    adView.loadAd(request);
                } catch (Throwable t) {
                    Log.w(TAG, "loadAd after init", t);
                }
            }));
        } catch (Throwable t) {
            Log.w(TAG, "MobileAds.initialize", t);
            adView.post(() -> {
                try {
                    adView.loadAd(request);
                } catch (Throwable t2) {
                    Log.w(TAG, "loadAd fallback", t2);
                }
            });
        }
    }
}
